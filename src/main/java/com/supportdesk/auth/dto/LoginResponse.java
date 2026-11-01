package com.supportdesk.auth.dto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST /api/auth/login. Who logged in, and which screens they may open, comes from GET /api/auth/me.
 */
public record LoginResponse(
        @Schema(description = "Send as the Authorization header: Bearer <accessToken>.")
        String accessToken,

        @Schema(description = "Trade for a new access token at POST /api/auth/refresh. Deleted on logout.")
        String refreshToken)
{
}
