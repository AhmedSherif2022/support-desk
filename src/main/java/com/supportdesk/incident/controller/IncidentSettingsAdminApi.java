package com.supportdesk.incident.controller;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.incident.dto.IncidentDtos.IncidentSettingsDto;
import com.supportdesk.incident.dto.IncidentDtos.UpdateIncidentSettingsRequest;
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
 * HTTP contract for the incident grouping thresholds (E6-S3): the Guide Admin reads and changes them.
 * {@link IncidentSettingsAdminController} implements this and delegates.
 */
@RequestMapping("/api/admin/incident-settings")
@Tag(name = "Incident settings", description = "How many different customers, within how many minutes, turn the same "
        + "error on the same platform into one incident. Guide Admin only. Defaults: 5 customers, 15 minutes.")
public interface IncidentSettingsAdminApi
{
    @GetMapping
    @Operation(summary = "Current grouping thresholds")
    @ApiResponse(responseCode = "200", description = "The thresholds in force")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    IncidentSettingsDto current();

    @PutMapping
    @Operation(summary = "Change the grouping thresholds",
            description = "Both values are whole numbers of at least 1. The next monitor run uses them. Each changed "
                    + "value is audited as SETTING_CHANGED (entity APP_SETTING) with the old and new value; an "
                    + "unchanged value records nothing.")
    @ApiResponse(responseCode = "200", description = "The thresholds now in force")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: a value is missing, zero, negative or too "
            + "large (fieldErrors says which); MALFORMED_REQUEST: the body is not JSON",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a GUIDE_ADMIN",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    IncidentSettingsDto update(@Valid @RequestBody UpdateIncidentSettingsRequest request);
}
