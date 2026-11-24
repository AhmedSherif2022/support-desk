package com.supportdesk.health.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthSettingsDto;
import com.supportdesk.health.dto.ServiceHealthDtos.UpdateServiceHealthSettingsRequest;
import com.supportdesk.incident.entity.AppSetting;
import com.supportdesk.incident.repository.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.supportdesk.health.service.ServiceHealthSettingsService.MIN_CUSTOMERS_KEY;
import static com.supportdesk.health.service.ServiceHealthSettingsService.WINDOW_MINUTES_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceHealthSettingsServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

    private final AppSettingRepository repository = mock(AppSettingRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private ServiceHealthSettingsService service;

    @BeforeEach
    void setUp()
    {
        service = new ServiceHealthSettingsService(repository, auditService, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void defaultsAreFiveCustomersAndFifteenMinutes()
    {
        assertThat(service.current()).isEqualTo(new ServiceHealthSettingsDto(5, 15));
    }

    @Test
    void aChangeIsAuditedAndAnUnchangedValueIsNot()
    {
        AppSetting window = new AppSetting(WINDOW_MINUTES_KEY, "15", NOW);
        when(repository.findById(WINDOW_MINUTES_KEY)).thenReturn(Optional.of(window));

        service.update(new UpdateServiceHealthSettingsRequest(3, 15));

        assertThat(window.getValue()).isEqualTo("15");
        verify(repository).save(any(AppSetting.class));
        verify(auditService).recordForCurrentUser(eq(AuditAction.SETTING_CHANGED),
                eq(ServiceHealthSettingsService.ENTITY), eq(MIN_CUSTOMERS_KEY), any(), any());
        verify(auditService, never()).recordForCurrentUser(eq(AuditAction.SETTING_CHANGED),
                eq(ServiceHealthSettingsService.ENTITY), eq(WINDOW_MINUTES_KEY), any(), any());
    }

    @Test
    void zeroIsRejected()
    {
        assertThatThrownBy(() -> service.update(new UpdateServiceHealthSettingsRequest(0, 15)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
