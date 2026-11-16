package com.supportdesk.ticket.controller;

import com.supportdesk.common.exception.ApiError;
import com.supportdesk.ticket.dto.TicketDtos.TakeStepRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * HTTP contract for the Reviewer's ticket review: approve or reject the SRE fixes that are In Review.
 * {@link TicketReviewController} only implements this and delegates.
 */
@RequestMapping("/api/review/tickets")
@Tag(name = "Ticket review", description = "Reviewer: approve or reject SRE fixes waiting In Review")
public interface TicketReviewApi
{
    @GetMapping
    @Operation(summary = "Tickets waiting for review",
            description = "Tickets In Review, oldest first by creation time. availableSteps on each is "
                    + "REVIEWER_APPROVES and REVIEWER_REJECTS. Tickets reach In Review through the system step "
                    + "SEND_TO_REVIEW, which comes with the Azure DevOps integration (not built yet), so this list "
                    + "stays empty until then.")
    @ApiResponse(responseCode = "200", description = "Tickets In Review")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a REVIEWER",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<TicketDto> inReview();

    @PostMapping("/{number}/steps")
    @Operation(summary = "Approve or reject the SRE fix",
            description = "REVIEWER_APPROVES moves the ticket from In Review to Resolved. REVIEWER_REJECTS needs a "
                    + "reason and moves it to Reopened, from where the system step FIX_FAILED sends it back to SRE "
                    + "(Azure DevOps integration, not built yet). " + "A step taken is audited as TICKET_STATUS_CHANGED with the step and the reason, if one was given. A step "
                    + "that does not start from the ticket's status is refused with INVALID_STATUS_TRANSITION and "
                    + "audited as TICKET_TRANSITION_REJECTED; other refusals are not audited.")
    @ApiResponse(responseCode = "200", description = "Updated ticket")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED (no step, or a reason over 500 characters) "
            + "or MALFORMED_REQUEST (unknown step)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not a REVIEWER",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND: no ticket with this number",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "DATA_CONFLICT: someone else changed the ticket at the same "
            + "time; reload it and try again",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_STATUS_TRANSITION: the ticket is not In Review; "
            + "STEP_NOT_YOURS: not a reviewer step; REASON_REQUIRED: a rejection without a reason",
            content = @Content(schema = @Schema(implementation = ApiError.class), examples = {
                    @ExampleObject(name = "Not In Review", value = "{\"code\":\"INVALID_STATUS_TRANSITION\",\"message\":\"REVIEWER_APPROVES starts from IN_REVIEW, but the ticket is RESOLVED.\",\"fieldErrors\":{},\"path\":\"/api/review/tickets/2001/steps\",\"timestamp\":\"2026-11-15T10:00:00Z\"}"),
                    @ExampleObject(name = "Not your step", value = "{\"code\":\"STEP_NOT_YOURS\",\"message\":\"APPLY_FIX can only be taken by AGENT.\",\"fieldErrors\":{},\"path\":\"/api/review/tickets/2001/steps\",\"timestamp\":\"2026-11-15T10:00:00Z\"}"),
                    @ExampleObject(name = "No reason", value = "{\"code\":\"REASON_REQUIRED\",\"message\":\"Say why the fix is rejected.\",\"fieldErrors\":{},\"path\":\"/api/review/tickets/2001/steps\",\"timestamp\":\"2026-11-15T10:00:00Z\"}")}))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = "Approve", value = "{\"step\": \"REVIEWER_APPROVES\"}"),
            @ExampleObject(name = "Reject", value = "{\"step\": \"REVIEWER_REJECTS\", \"reason\": \"Login still fails after the deploy.\"}")}))
    TicketDto takeStep(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001")
            @PathVariable Long number,
            @Valid @RequestBody TakeStepRequest request);
}
