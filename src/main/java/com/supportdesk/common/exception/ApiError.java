package com.supportdesk.common.exception;
import java.time.Instant;
import java.util.Map;

public record ApiError
        (
        String code,
        String message,
        Map<String, String> fieldErrors,
        String path,
        Instant timestamp
)
{

    public static ApiError of(String code, String message, String path)
    {
        return new ApiError(code, message, Map.of(), path, Instant.now());
    }

    public static ApiError withFields(String code, String message, Map<String, String> fieldErrors, String path)
    {
        return new ApiError(code, message, fieldErrors, path, Instant.now());
    }
}
