package com.supportdesk.monitor.config;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;


@Validated
@ConfigurationProperties(prefix = "supportdesk.monitor")
public record MonitorProperties(
        boolean enabled,
        @Min(1) int batchSize,
        @NotNull Duration duplicateWindow)
{
}