package com.supportdesk.incident.controller;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.incident.dto.IncidentDtos.IncidentDto;
import com.supportdesk.incident.dto.IncidentDtos.IncidentTicketDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * HTTP contract for incidents as the Agent sees them (E6-S3): read only. Incidents are opened by the monitor job;
 * their later lifecycle and the Azure work item belong to E5 (postponed). {@link IncidentController} implements this.
 */
@RequestMapping("/api/incidents")
@Tag(name = "Incidents", description = "Mass failures grouped into one problem: the same error code on the same "
        + "platform hit by many different customers within the grouping window. Agents read; the job writes.")
public interface IncidentApi
{
    @GetMapping
    @Operation(summary = "Incidents, newest first",
            description = "Defaults to the OPEN ones; pass status=SRE_FIXED or RESOLVED for the others, or "
                    + "status=ALL for every incident. Each row carries the affected customer count and the number of "
                    + "linked tickets. Paging: page (from 0), size (default 20, max 100).")
    @ApiResponse(responseCode = "200", description = "One page of incidents")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: status is not OPEN, SRE_FIXED, RESOLVED or ALL",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<IncidentDto> list(
            @Parameter(description = "OPEN (default), SRE_FIXED, RESOLVED or ALL.", example = "OPEN")
            @RequestParam(required = false, defaultValue = "OPEN") String status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable);

    @GetMapping("/{id}")
    @Operation(summary = "One incident", description = "Error code, platform, service, status, affected customer "
            + "count, ticket count and when its errors started and were last seen.")
    @ApiResponse(responseCode = "200", description = "The incident")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: id is not a number",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "INCIDENT_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    IncidentDto get(@Parameter(description = "Incident id.", example = "7") @PathVariable Long id);

    @GetMapping("/{id}/tickets")
    @Operation(summary = "Tickets linked to an incident",
            description = "In the order they were linked. Each names the customer by CIF and name only.")
    @ApiResponse(responseCode = "200", description = "The linked tickets")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "INCIDENT_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<IncidentTicketDto> tickets(@Parameter(description = "Incident id.", example = "7") @PathVariable Long id);
}
