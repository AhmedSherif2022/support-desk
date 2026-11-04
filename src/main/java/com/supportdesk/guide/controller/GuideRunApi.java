package com.supportdesk.guide.controller;

import com.supportdesk.common.exception.ApiError;
import com.supportdesk.guide.dto.GuideDtos.AnswerRequest;
import com.supportdesk.guide.dto.GuideDtos.GuideRunDto;
import com.supportdesk.guide.dto.GuideDtos.StartGuideRunRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * HTTP contract for running a guide on a call. {@link GuideRunController} only implements this and delegates.
 * Every call returns the whole run, so the screen always redraws from what the server holds.
 */
@RequestMapping("/api/guide-runs")
@PreAuthorize("hasRole('AGENT')")
@Tag(name = "Guide runs", description = "Run a guide one question at a time: start it on a ticket, answer the current step, "
        + "and go back. A run stays on the version it started on.")
public interface GuideRunApi
{
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a guide",
            description = "Starts the guide's live version on the ticket, moves the ticket to Troubleshooting and audits "
                    + "GUIDE_STARTED with the version.")
    @ApiResponse(responseCode = "201", description = "Run started, on its first step")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or MALFORMED_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND, or GUIDE_NOT_FOUND (unknown guide or none live)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "GUIDE_RUN_ALREADY_ACTIVE: the ticket has an unfinished run",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "GUIDE_DOES_NOT_FIT (not a guide for this ticket), or INVALID_STATUS_TRANSITION (the ticket cannot move to Troubleshooting)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    GuideRunDto start(@Valid @RequestBody StartGuideRunRequest request);

    @GetMapping("/{id}")
    @Operation(summary = "Get a run",
            description = "The question, options, answers so far, progress, hint, and the outcome once reached.")
    @ApiResponse(responseCode = "200", description = "The run")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "GUIDE_RUN_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    GuideRunDto get(@Parameter(example = "17") @PathVariable Long id);

    @PostMapping("/{id}/answers")
    @Operation(summary = "Answer the current step",
            description = "Checks the answer against the run's version and moves to the next step or to the outcome.")
    @ApiResponse(responseCode = "200", description = "The run, on its next step or at its outcome")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: stepKey or answerKey missing",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "GUIDE_RUN_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "GUIDE_RUN_NOT_ACTIVE (finished), or DATA_CONFLICT (answered twice at once)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_GUIDE_ROUTE: not the current step, or not one of its answers",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    GuideRunDto answer(@Parameter(example = "17") @PathVariable Long id, @Valid @RequestBody AnswerRequest request);

    @DeleteMapping("/{id}/answers/{position}")
    @Operation(summary = "Go back",
            description = "Returns to the step answered at this position and clears that answer and every later one. "
                    + "Previous sends the last position. Sending the same position again changes nothing.")
    @ApiResponse(responseCode = "200", description = "The run, back on that step")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "GUIDE_RUN_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "GUIDE_RUN_NOT_ACTIVE: the run is finished",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_GUIDE_ROUTE: position below 1",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    GuideRunDto clearFrom(@Parameter(example = "17") @PathVariable Long id,
                          @Parameter(description = "1-based, from the run's path.", example = "2") @PathVariable int position);
}
