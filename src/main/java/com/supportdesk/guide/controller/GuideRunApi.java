package com.supportdesk.guide.controller;

import com.supportdesk.common.exception.ApiError;
import com.supportdesk.guide.dto.GuideDtos.AnswerRequest;
import com.supportdesk.guide.dto.GuideDtos.CompleteRunRequest;      // E4-S3
import com.supportdesk.guide.dto.GuideDtos.CompletedRunDto;         // E4-S3
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

@RequestMapping("/api/guide-runs")
@PreAuthorize("hasRole('AGENT')")
@Tag(name = "Guide runs", description = "Run a guide one question at a time: start it on a ticket, answer the current step, "
        + "go back, and finish it on its outcome. A run stays on the version it started on.")   // E4-S3: "finish"
public interface GuideRunApi
{
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a guide",
            description = "Starts the guide's live version on the ticket and audits GUIDE_STARTED with the version. "
                    + "The ticket moves to Troubleshooting by the agent's step: PICK_UP_TICKET from New, "
                    + "DETAILS_RECEIVED from Waiting on Customer, none if already Troubleshooting. Any other status "
                    + "is refused with INVALID_STATUS_TRANSITION. A step taken by hand during a run (for example "
                    + "REQUEST_DETAILS) leaves the run unfinished; starting a guide from Waiting on Customer abandons it "
                    + "and starts a new one. Taking DETAILS_RECEIVED by hand instead brings the ticket back to "
                    + "Troubleshooting with that run in activeGuideRunId, and a new start gets "
                    + "GUIDE_RUN_ALREADY_ACTIVE: reopen it. After a reload, find the unfinished run in the "
                    + "ticket's activeGuideRunId.")
    @ApiResponse(responseCode = "201", description = "Run started, on its first step")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or MALFORMED_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND, or GUIDE_NOT_FOUND (unknown guide or none live)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "GUIDE_RUN_ALREADY_ACTIVE: the ticket is Troubleshooting and already has an "
            + "unfinished run; reopen it from the ticket's activeGuideRunId",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "GUIDE_DOES_NOT_FIT (not a guide for this ticket), or INVALID_STATUS_TRANSITION (the ticket is not New, Troubleshooting or Waiting on Customer)",
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
    GuideRunDto get(@Parameter(description = "Guide run id, from starting the run.", example = "17") @PathVariable Long id);

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
    @ApiResponse(responseCode = "409", description = "GUIDE_RUN_NOT_ACTIVE: the run is finished or was abandoned",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_GUIDE_ROUTE: not the current step, or not one of its answers. "
            + "The same answer sent twice at once gets this too: the second finds the run already moved on.",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    GuideRunDto answer(@Parameter(description = "Guide run id, from starting the run.", example = "17") @PathVariable Long id, @Valid @RequestBody AnswerRequest request);

    @DeleteMapping("/{id}/answers/{position}")
    @Operation(summary = "Go back",
            description = "Returns to the step answered at this position and clears that answer and every later one. "
                    + "The screen's Previous button sends the last position. A position past the last answer, such as "
                    + "the same position sent again, changes nothing.")
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
    GuideRunDto clearFrom(@Parameter(description = "Guide run id, from starting the run.", example = "17") @PathVariable Long id,
                          @Parameter(description = "1-based, from the run's path.", example = "2") @PathVariable int position);



    @PostMapping("/{id}/complete")
    @Operation(summary = "Finish a guide",
            description = "Applies the reached outcome to the ticket as a guide step:\n"
                    + "- Resolved: GUIDE_SOLVED, to RESOLVED\n"
                    + "- Follow-up: GUIDE_FOLLOW_UP, to WAITING_ON_CUSTOMER (the SLA clock pauses)\n"
                    + "- Escalate: GUIDE_HAND_OVER, to ESCALATED\n\n"
                    + "It also saves the outcome and the notes on the run, audits GUIDE_COMPLETED with the session "
                    + "summary (its fields are listed on the ticket Audit tab), and "
                    + "logs the customer script as \"Told customer: …\".")
    @ApiResponse(responseCode = "200", description = "Run completed, with the outcome applied and the ticket's new status")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: notes longer than 2000 characters, or MALFORMED_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "GUIDE_RUN_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "GUIDE_OUTCOME_NOT_REACHED: the questions are not finished, or the run is already completed or was abandoned",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_STATUS_TRANSITION: the ticket is no longer Troubleshooting (it was moved by a step during the run)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CompletedRunDto complete(@Parameter(description = "Guide run id, from starting the run.", example = "17") @PathVariable Long id,
                             @Valid @RequestBody CompleteRunRequest request);
}
