package com.supportdesk.auth.dto;

import java.time.Instant;

public record LoginResponse(String token, String refreshToken, String tokenType, Instant expiresAt, UserDto user)
{
    public static LoginResponse bearer(String token, String refreshToken, Instant expiresAt, UserDto user)
    {
        return new LoginResponse(token, refreshToken, "Bearer", expiresAt, user);
    }

    public static LoginResponse bearer(String token, Instant expiresAt, UserDto user)
    {
        return new LoginResponse(token, null, "Bearer", expiresAt, user);
    }
}
