package com.supportdesk.health.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthSettingsDto;
import com.supportdesk.health.dto.ServiceHealthDtos.UpdateServiceHealthSettingsRequest;
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
 * The DEGRADED threshold a Guide Admin can tune. Separate keys from incident grouping, so changing one
 * does not change the other. Read on every home-panel request. Each change is audited as SETTING_CHANGED.
 */
@Service
@RequiredArgsConstructor
public class ServiceHealthSettingsService
{
    public static final String ENTITY = "APP_SETTING";
    public static final String MIN_CUSTOMERS_KEY = "service-health.degraded.min-customers";
    public static final String WINDOW_MINUTES_KEY = "service-health.degraded.window-minutes";

    static final int DEFAULT_MIN_CUSTOMERS = 5;
    static final int DEFAULT_WINDOW_MINUTES = 15;

    private final AppSettingRepository settingRepository;
    private final AuditService auditService;
    private final Clock clock;


    @Transactional(readOnly = true)
    public ServiceHealthSettingsDto current()
    {
        return new ServiceHealthSettingsDto(minCustomers(), windowMinutes());
    }

    @Transactional(readOnly = true)
    public int minCustomers()
    {
        return readPositiveInt(MIN_CUSTOMERS_KEY, DEFAULT_MIN_CUSTOMERS);
    }

    @Transactional(readOnly = true)
    public Duration window()
    {
        return Duration.ofMinutes(windowMinutes());
    }

    private int windowMinutes()
    {
        return readPositiveInt(WINDOW_MINUTES_KEY, DEFAULT_WINDOW_MINUTES);
    }


    @Transactional
    public ServiceHealthSettingsDto update(UpdateServiceHealthSettingsRequest request)
    {
        requirePositive(request.minCustomers(), "minimum number of customers");
        requirePositive(request.windowMinutes(), "degraded window");

        Instant now = clock.instant();
        change(MIN_CUSTOMERS_KEY, request.minCustomers(), now);
        change(WINDOW_MINUTES_KEY, request.windowMinutes(), now);
        return current();
    }

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
