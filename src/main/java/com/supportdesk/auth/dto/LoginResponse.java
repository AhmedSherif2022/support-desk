package com.supportdesk.auth.dto;

public record LoginResponse(String accessToken, String refreshToken)
{
}
