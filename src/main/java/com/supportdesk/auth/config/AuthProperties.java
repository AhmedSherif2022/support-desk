package com.supportdesk.auth.config;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;


@Validated

@ConfigurationProperties(prefix = "supportdesk.auth")

public record AuthProperties
        (
        @NotBlank String jwtSecret,          // must not be empty
        @NotNull Duration sessionTimeout,    // must be present, e.g. 30m
        @Min(1) int maxFailedAttempts,       // at least 1, e.g. 5
        @NotNull Duration lockDuration       // must be present, e.g. 15m
        )
{
}
