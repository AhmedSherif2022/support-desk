package com.supportdesk.azure.controller;

import com.supportdesk.azure.dto.EscalationDtos.EscalateRequest;
import com.supportdesk.azure.dto.EscalationDtos.EscalationDto;
import com.supportdesk.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/** HTTP contract for escalating a ticket to SRE (E5-S1, E5-S3). {@link EscalationController} implements it. */
@RequestMapping("/api/tickets/{number}/escalation")
@Tag(name = "Escalation to SRE", description = "Agents escalate a ticket to SRE in Azure DevOps. The work item is "
        + "sent in the background and retried while Azure is unavailable, so escalating never waits for Azure.")
public interface EscalationApi
{
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Escalate the ticket to SRE",
            description = "Reason and urgency are required. The ticket moves from TROUBLESHOOTING to ESCALATED, and an "
                    + "Azure DevOps work item is queued in the same transaction (syncStatus PENDING). The work item "
                    + "carries the ticket ID and URL, a masked customer token, platform, service, category, error code, "
                    + "summary, log snapshot, correlation ID, priority, SLA, guide path and reason; never CIF, national "
                    + "ID or mobile. When Azure confirms, syncStatus becomes SYNCED with the work item number. Audited "
                    + "as TICKET_ESCALATED.")
    @ApiResponse(responseCode = "201", description = "Escalated; the work item is on its way")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: reason (10 to 500 characters) or urgency missing",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "ALREADY_ESCALATED: the ticket already has an escalation",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_STATUS_TRANSITION: only a ticket in TROUBLESHOOTING "
            + "can be escalated (pick it up first)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    EscalationDto escalate(@Parameter(example = "2001") @PathVariable Long number,
                           @Valid @RequestBody EscalateRequest request);

    @GetMapping
    @Operation(summary = "The ticket's escalation and sync state",
            description = "syncStatus PENDING shows \"pending · retrying\" (no action needed), SYNCED shows the work "
                    + "item number and Azure state, FAILED means the failed-integration alert fired.")
    @ApiResponse(responseCode = "200", description = "The escalation")
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND or ESCALATION_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    EscalationDto get(@Parameter(example = "2001") @PathVariable Long number);
}
