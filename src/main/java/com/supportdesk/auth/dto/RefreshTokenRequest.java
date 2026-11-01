package com.supportdesk.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenRequest
{

    @Schema(description = "The refreshToken from POST /api/auth/login.")
    private String refreshToken;
}
