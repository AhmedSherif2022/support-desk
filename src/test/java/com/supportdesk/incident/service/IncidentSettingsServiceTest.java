package com.supportdesk.incident.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.incident.dto.IncidentDtos.IncidentSettingsDto;
import com.supportdesk.incident.dto.IncidentDtos.UpdateIncidentSettingsRequest;
import com.supportdesk.incident.entity.AppSetting;
import com.supportdesk.incident.repository.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static com.supportdesk.incident.service.IncidentSettingsService.MIN_CUSTOMERS_KEY;
import static com.supportdesk.incident.service.IncidentSettingsService.WINDOW_MINUTES_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IncidentSettingsServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private final AppSettingRepository repository = mock(AppSettingRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private IncidentSettingsService service;

    @BeforeEach
    void setUp()
    {
        service = new IncidentSettingsService(repository, auditService, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.findById(any())).thenReturn(Optional.empty());
    }


    // ---- defaults ----

    @Test
    void defaultMinimumAffectedCustomersIsFive()
    {
        assertThat(service.minAffectedCustomers()).isEqualTo(5);
        assertThat(service.current().minAffectedCustomers()).isEqualTo(5);
    }

    @Test
    void defaultGroupingWindowIsFifteenMinutes()
    {
        assertThat(service.groupingWindow()).isEqualTo(Duration.ofMinutes(15));
        assertThat(service.current().groupingWindowMinutes()).isEqualTo(15);
    }

    @Test
    void storedValuesWinOverTheDefaults()
    {
        stored(MIN_CUSTOMERS_KEY, "3");
        stored(WINDOW_MINUTES_KEY, "30");

        assertThat(service.current()).isEqualTo(new IncidentSettingsDto(3, 30));
        assertThat(service.groupingWindow()).isEqualTo(Duration.ofMinutes(30));
    }

    @ParameterizedTest(name = "stored \"{0}\" falls back to the default")
    @ValueSource(strings = {"0", "-4", "abc", " "})
    void aCorruptStoredValueFallsBackToTheDefault(String value)
    {
        stored(MIN_CUSTOMERS_KEY, value);

        assertThat(service.minAffectedCustomers()).isEqualTo(5);
    }


    // ---- updates ----

    @Test
    void validSettingsAreSavedAndEachChangeIsAuditedWithOldAndNewValue()
    {
        AppSetting min = stored(MIN_CUSTOMERS_KEY, "5");
        AppSetting window = stored(WINDOW_MINUTES_KEY, "15");

        IncidentSettingsDto result = service.update(new UpdateIncidentSettingsRequest(3, 30));

        assertThat(result).isEqualTo(new IncidentSettingsDto(3, 30));
        assertThat(min.getValue()).isEqualTo("3");
        assertThat(min.getUpdatedAt()).isEqualTo(NOW);
        assertThat(window.getValue()).isEqualTo("30");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> before = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> after = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordForCurrentUser(eq(AuditAction.SETTING_CHANGED), eq(IncidentSettingsService.ENTITY),
                eq(MIN_CUSTOMERS_KEY), before.capture(), after.capture());
        assertThat(before.getValue()).containsEntry("value", "5");
        assertThat(after.getValue()).containsEntry("value", "3");
        verify(auditService).recordForCurrentUser(eq(AuditAction.SETTING_CHANGED), eq(IncidentSettingsService.ENTITY),
                eq(WINDOW_MINUTES_KEY), any(), any());
    }

    @Test
    void anUnchangedValueIsNeitherWrittenNorAudited()
    {
        stored(MIN_CUSTOMERS_KEY, "5");
        AppSetting window = stored(WINDOW_MINUTES_KEY, "15");

        service.update(new UpdateIncidentSettingsRequest(5, 20));

        assertThat(window.getValue()).isEqualTo("20");
        verify(auditService, times(1)).recordForCurrentUser(any(), any(), any(), any(), any());
        verify(auditService).recordForCurrentUser(eq(AuditAction.SETTING_CHANGED), any(), eq(WINDOW_MINUTES_KEY), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void aMissingRowIsCreatedOnUpdate()
    {
        service.update(new UpdateIncidentSettingsRequest(8, 10));

        ArgumentCaptor<AppSetting> saved = ArgumentCaptor.forClass(AppSetting.class);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(AppSetting::getKey, AppSetting::getValue)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(MIN_CUSTOMERS_KEY, "8"),
                        org.assertj.core.groups.Tuple.tuple(WINDOW_MINUTES_KEY, "10"));
    }

    @ParameterizedTest(name = "minimum customers {0} is refused")
    @ValueSource(ints = {0, -1, -100})
    void aNonPositiveMinimumIsRefusedAndNothingChanges(int bad)
    {
        assertThatThrownBy(() -> service.update(new UpdateIncidentSettingsRequest(bad, 15)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_SETTING_VALUE");

        verify(repository, never()).save(any());
        verify(auditService, never()).recordForCurrentUser(any(), any(), any(), any(), any());
    }

    @ParameterizedTest(name = "window {0} is refused")
    @ValueSource(ints = {0, -15})
    void aNonPositiveWindowIsRefusedAndNothingChanges(int bad)
    {
        assertThatThrownBy(() -> service.update(new UpdateIncidentSettingsRequest(5, bad)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_SETTING_VALUE");

        verify(repository, never()).save(any());
        verify(auditService, never()).recordForCurrentUser(any(), any(), any(), any(), any());
    }

    @Test
    void aMissingValueIsRefused()
    {
        assertThatThrownBy(() -> service.update(new UpdateIncidentSettingsRequest(null, 15)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.update(new UpdateIncidentSettingsRequest(5, null)))
                .isInstanceOf(BusinessRuleException.class);
    }


    private AppSetting stored(String key, String value)
    {
        AppSetting setting = new AppSetting(key, value, NOW.minusSeconds(3600));
        when(repository.findById(key)).thenReturn(Optional.of(setting));
        return setting;
    }
}
