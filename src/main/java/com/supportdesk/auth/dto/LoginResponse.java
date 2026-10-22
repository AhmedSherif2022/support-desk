package com.supportdesk.auth.dto;
import java.time.Instant;


public record LoginResponse(String token, String tokenType, Instant expiresAt, UserDto user)
{
    public static LoginResponse bearer(String token, Instant expiresAt, UserDto user)
    {
        return new LoginResponse(token, "Bearer", expiresAt, user);
    }
}
