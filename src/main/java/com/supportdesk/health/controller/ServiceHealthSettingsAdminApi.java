package com.supportdesk.health.controller;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthSettingsDto;
import com.supportdesk.health.dto.ServiceHealthDtos.UpdateServiceHealthSettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HTTP contract for the DEGRADED threshold. Not the incident grouping settings: those live at
 * /api/admin/incident-settings and count a different thing.
 */
@RequestMapping("/api/admin/service-health-settings")
@Tag(name = "Service health settings", description = "How many distinct customers, within how many minutes, "
        + "mark a bank service DEGRADED on the agent home panel. Guide Admin only. Defaults: 5 customers, 15 minutes. "
        + "Independent of the incident grouping thresholds.")
public interface ServiceHealthSettingsAdminApi
{
    @GetMapping
    @Operation(summary = "Current degraded thresholds")
    @ApiResponse(responseCode = "200", description = "The thresholds in force")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ServiceHealthSettingsDto current();

    @PutMapping
    @Operation(summary = "Change the degraded thresholds",
            description = "Both values are whole numbers of at least 1. The next home-panel read uses them. "
                    + "Each changed value is audited as SETTING_CHANGED (entity APP_SETTING) with the old and new "
                    + "value; an unchanged value records nothing.")
    @ApiResponse(responseCode = "200", description = "The thresholds now in force")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: a value is missing, zero, negative or too "
            + "large (fieldErrors says which); MALFORMED_REQUEST: the body is not JSON",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ServiceHealthSettingsDto update(@Valid @RequestBody UpdateServiceHealthSettingsRequest request);
}
