package com.supportdesk.incident.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.incident.dto.IncidentDtos.IncidentSettingsDto;
import com.supportdesk.incident.dto.IncidentDtos.UpdateIncidentSettingsRequest;
import com.supportdesk.incident.entity.AppSetting;
import com.supportdesk.incident.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * E6-S3: the grouping thresholds the Guide Admin can tune. Values live in app_settings (seeded by migration) and are
 * read on every grouping run, so a change applies to the next monitor run without a restart. Each change is audited
 * as SETTING_CHANGED with the old and new value.
 */
@Service
@RequiredArgsConstructor
public class IncidentSettingsService
{
    public static final String ENTITY = "APP_SETTING";
    public static final String MIN_CUSTOMERS_KEY = "incident.grouping.min-customers";
    public static final String WINDOW_MINUTES_KEY = "incident.grouping.window-minutes";

    // Used only when the row is missing (e.g. deleted by hand); the migration seeds the same values.
    static final int DEFAULT_MIN_CUSTOMERS = 5;
    static final int DEFAULT_WINDOW_MINUTES = 15;

    private final AppSettingRepository settingRepository;
    private final AuditService auditService;
    private final Clock clock;


    @Transactional(readOnly = true)
    public IncidentSettingsDto current()
    {
        return new IncidentSettingsDto(minAffectedCustomers(), groupingWindowMinutes());
    }

    @Transactional(readOnly = true)
    public int minAffectedCustomers()
    {
        return readPositiveInt(MIN_CUSTOMERS_KEY, DEFAULT_MIN_CUSTOMERS);
    }

    @Transactional(readOnly = true)
    public Duration groupingWindow()
    {
        return Duration.ofMinutes(groupingWindowMinutes());
    }

    private int groupingWindowMinutes()
    {
        return readPositiveInt(WINDOW_MINUTES_KEY, DEFAULT_WINDOW_MINUTES);
    }


    @Transactional
    public IncidentSettingsDto update(UpdateIncidentSettingsRequest request)
    {
        requirePositive(request.minAffectedCustomers(), "minimum number of affected customers");
        requirePositive(request.groupingWindowMinutes(), "grouping window");

        Instant now = clock.instant();
        change(MIN_CUSTOMERS_KEY, request.minAffectedCustomers(), now);
        change(WINDOW_MINUTES_KEY, request.groupingWindowMinutes(), now);
        return current();
    }

    /** Writes one setting if its value differs and audits the change; an unchanged value records nothing. */
    private void change(String key, int newValue, Instant now)
    {
        AppSetting setting = settingRepository.findById(key).orElse(null);
        String before = setting == null ? null : setting.getValue();
        String after = String.valueOf(newValue);
        if (after.equals(before)) {
            return;
        }
        if (setting == null) {
            settingRepository.save(new AppSetting(key, after, now));
        } else {
            setting.change(after, now);
        }
        auditService.recordForCurrentUser(AuditAction.SETTING_CHANGED, ENTITY, key,
                Map.of("key", key, "value", before == null ? "" : before),
                Map.of("key", key, "value", after));
    }

    private int readPositiveInt(String key, int fallback)
    {
        return settingRepository.findById(key)
                .map(AppSetting::getValue)
                .map(value -> {
                    try {
                        int parsed = Integer.parseInt(value.trim());
                        return parsed > 0 ? parsed : fallback;
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private static void requirePositive(Integer value, String name)
    {
        if (value == null || value <= 0) {
            throw new BusinessRuleException(ErrorCode.INVALID_SETTING_VALUE,
                    "The " + name + " must be a positive whole number.");
        }
    }
}
