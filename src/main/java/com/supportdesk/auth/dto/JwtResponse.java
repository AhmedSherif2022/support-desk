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
public class JwtResponse
{

    @Schema(description = "Valid 15 minutes. Send it as the Authorization header: `Bearer <accessToken>`.") 
    private String accessToken;
    @Schema(description = "The same refresh token, unchanged; its 30 minutes restart now.") 
    private String refreshToken;

}
