package com.supportdesk.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui.html, locally and in production (raw OpenAPI JSON: /v3/api-docs).
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
                        1. `POST /api/auth/login` with your staff account and copy `accessToken`.
                        2. Press **Authorize** and paste the token (no `Bearer ` prefix).
                        3. The access token lasts 15 minutes: get a new one with `POST /api/auth/refresh`.

                        **Agent flow** (role AGENT)
                        1. Find the caller with `GET /api/customers/search` and verify them with `POST /api/customers/{cif}/verify` (at least two checks, for example on `CIF-100245`).
                        2. Open a ticket: `GET /api/tickets/categories` for the form (each category's `defaultPriority`), `POST /api/tickets/precheck` for warnings, then `POST /api/tickets`.
                        3. Work it: `GET /api/tickets/{number}` shows the pinned next step and `availableSteps`; move it with `POST /api/tickets/{number}/steps`, or run a guide from `GET /api/guides/recommended` with `POST /api/guide-runs`.
                        4. Home: `GET /api/tickets/queue` (chips `ALL`, `AT_RISK`, `IN_REVIEW`, `AUTO_CREATED`) is the team's open tickets by SLA risk; `GET /api/tickets` is the agent's own list.

                        **Reviewer flow** (role REVIEWER)
                        `GET /api/review/tickets` lists the tickets In Review; `POST /api/review/tickets/{number}/steps` approves the SRE fix (`REVIEWER_APPROVES`, to Resolved) or rejects it with a reason (`REVIEWER_REJECTS`, to Reopened). Tickets reach In Review through the Azure DevOps integration (not built yet), so the list stays empty until then.

                        **Errors** always come back as `{code, message, fieldErrors, path, timestamp}`; login lockouts and limits add `retryAt`, `retryAfterSeconds` and `attemptsLeft` when they apply.
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
