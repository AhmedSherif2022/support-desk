package com.supportdesk.common.exception;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record ApiError
        (
        String code,
        String message,
        Map<String, String> fieldErrors,
        String path,
        Instant timestamp,

        @Schema(description = "Only when the request can be retried later (ACCOUNT_LOCKED): the earliest retry time.",
                example = "2026-11-12T10:15:00Z", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant retryAt,

        @Schema(description = "Only with retryAt: seconds left until then, for a countdown. Also sent as the Retry-After header.",
                example = "900", nullable = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long retryAfterSeconds
)
{

    public static ApiError of(String code, String message, String path)
    {
        return new ApiError(code, message, Map.of(), path, Instant.now(), null, null);
    }

    public static ApiError withFields(String code, String message, Map<String, String> fieldErrors, String path)
    {
        return new ApiError(code, message, fieldErrors, path, Instant.now(), null, null);
    }

    public static ApiError retryLater(String code, String message, String path, Instant retryAt, long retryAfterSeconds)
    {
        return new ApiError(code, message, Map.of(), path, Instant.now(), retryAt, retryAfterSeconds);
    }
}
