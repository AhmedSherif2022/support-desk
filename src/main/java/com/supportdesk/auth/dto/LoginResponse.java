package com.supportdesk.auth.dto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;


/**
 * POST /api/auth/login: the tokens plus who logged in, so the frontend can build its menu without calling /me.
 */
public record LoginResponse(
        @Schema(example = "Login successful.")
        String message,

        @Schema(description = "Send as the Authorization header: Bearer <accessToken>.")
        String accessToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "When the access token stops working; renew it with POST /api/auth/refresh.")
        Instant expiresAt,

        @Schema(description = "Trade for a new access token at POST /api/auth/refresh. Deleted on logout.")
        String refreshToken,

        @Schema(description = "Same as GET /api/auth/me.")
        MeResponse user)
{
    public static LoginResponse bearer(String accessToken, Instant expiresAt, String refreshToken, MeResponse user)
    {
        return new LoginResponse("Login successful.", accessToken, "Bearer", expiresAt, refreshToken, user);
    }
}
