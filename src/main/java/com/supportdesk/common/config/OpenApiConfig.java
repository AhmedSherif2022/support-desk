package com.supportdesk.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html  (raw OpenAPI JSON: /v3/api-docs).
 * Every endpoint needs the bearer token unless it says otherwise (login and refresh opt out).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Support Desk API",
                version = "v1",
                description = """
                        Backend of the internal customer-service desk. There is no sign-up: staff accounts are seeded.

                        **Calling a protected endpoint**
                        1. `POST /api/auth/login` and copy `accessToken` from the response.
                        2. Press **Authorize** and paste the token (no `Bearer ` prefix).
                        3. When it expires, get a new one with `POST /api/auth/refresh`.

                        **Errors** always come back as the same JSON: `{code, message, fieldErrors, path, timestamp}`.
                        `401 UNAUTHORIZED` means no or expired token; `403 FORBIDDEN` means your role may not open that area
                        (every 403 is written to the audit log).
                        """),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "The accessToken from POST /api/auth/login.")
public class OpenApiConfig
{
    public static final String BEARER_AUTH = "bearerAuth";
}
