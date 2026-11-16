package com.supportdesk.ticket.controller;

import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.ticket.enums.TicketListView;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactDto;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactRequest;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactsResponse;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.HistoryResponse;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.TicketListRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.supportdesk.ticket.enums.QueueChip;
import com.supportdesk.ticket.dto.QueueResponse;
import com.supportdesk.ticket.dto.TicketDtos.CategoryDto;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckDto;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckRequest;
import com.supportdesk.ticket.dto.TicketDtos.TakeStepRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/**
 * HTTP contract for tickets. {@link TicketController} only implements this and delegates.
 */
@RequestMapping("/api/tickets")
@PreAuthorize("hasRole('AGENT')")
@Tag(name = "Tickets", description = "Create tickets, move them through their lifecycle, work the home queue by SLA risk, log customer contact, and read each ticket's timeline and audit.")
public interface TicketApi
{
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a ticket",
            description = "Creates a ticket for a verified caller, assigns it to the current agent, and starts its SLA clock.")
    @ApiResponse(responseCode = "201", description = "Ticket created")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or MALFORMED_REQUEST: invalid or unreadable fields",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN (not an AGENT) or CALLER_NOT_VERIFIED (verify caller first)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND: unknown CIF",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "DUPLICATE_OPEN_TICKET: an open ticket already exists for this customer and category",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422",
            description = "CATEGORY_NOT_FOUND, CATEGORY_INACTIVE, SUBCATEGORY_REQUIRED, REFERENCE_REQUIRED, or REFERENCE_FORMAT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    TicketDto create(@Valid @RequestBody CreateTicketRequest request);

    @GetMapping("/categories")
    @Operation(summary = "Ticket categories",
            description = "Active categories for the create-ticket form, by name. A ticket uses a sub-category: a row "
                    + "with a parentCode. Start the priority from defaultPriority, and ask for a reference when "
                    + "referenceRequired is true.")
    @ApiResponse(responseCode = "200", description = "Active categories and sub-categories")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: not an AGENT",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<CategoryDto> categories();

    @PostMapping("/precheck")
    @Operation(summary = "Precheck before creating a ticket",
            description = "Checks whether an open ticket already exists for the verified customer and category so the UI can warn the agent.")
    @ApiResponse(responseCode = "200", description = "Precheck warnings (empty list when clear)")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: cif or categoryCode missing",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN (not an AGENT) or CALLER_NOT_VERIFIED",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "CUSTOMER_NOT_FOUND: unknown CIF",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "CATEGORY_NOT_FOUND: unknown categoryCode",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PrecheckDto precheck(@Valid @RequestBody PrecheckRequest request);

    @GetMapping("/queue")
    @Operation(summary = "Home queue",
            description = "Every agent's open tickets: the queue is shared by the team, not filtered to the current "
                    + "agent (My tickets is). Order: paused tickets last; otherwise the highest SLA % used first, then "
                    + "the least time left, then the lower id. Each row names one next action. Counts cover every chip "
                    + "even when one chip is selected. Every open ticket is returned (no paging).")
    @ApiResponse(responseCode = "200", description = "The open queue and chip counts")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: chip is not ALL, AT_RISK, IN_REVIEW, or AUTO_CREATED",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: this role cannot open the queue",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    QueueResponse queue(
            @Parameter(description = "ALL, AT_RISK (at risk or breached), IN_REVIEW (an SRE fix waiting for a reviewer: the screen's \"Confirm fix\" chip) or AUTO_CREATED. Omitted or empty means ALL.", example = "ALL")
            @RequestParam(required = false) QueueChip chip);

    @GetMapping("/{number}")
    @Operation(summary = "Get ticket by number",
            description = "The ticket, its SLA state, the steps the current user can take now (availableSteps), and "
                    + "any unfinished guide run (activeGuideRunId). The customer's name and CIF come without caller "
                    + "verification: verification guards Customer 360 only.")
    @ApiResponse(responseCode = "200", description = "The ticket")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: ticket number is not a valid number",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: this role cannot open tickets",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND: no ticket with this number",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    TicketDto get(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001")
            @PathVariable Long number);

    @PostMapping("/{number}/steps")
    @Operation(summary = "Take a step",
            description = "Moves the ticket by one agent step of the ticket diagram (see TicketStep): PICK_UP_TICKET, "
                    + "REQUEST_DETAILS, DETAILS_RECEIVED, APPLY_FIX or ARCHIVE. The ticket's availableSteps lists the "
                    + "ones open now. Any agent can take steps on any ticket (there is no assignee check), and "
                    + "PICK_UP_TICKET does not assign the ticket: assignedAgent stays the agent who created it. Entering "
                    + "Waiting on Customer pauses the SLA clock, leaving it resumes it, and Resolved stops it. "
                    + "A step taken is audited as TICKET_STATUS_CHANGED with the step and the reason, if one was given. A step "
                    + "that does not start from the ticket's status is refused with INVALID_STATUS_TRANSITION and "
                    + "audited as TICKET_TRANSITION_REJECTED; other refusals are not audited.")
    @ApiResponse(responseCode = "200", description = "Updated ticket")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED (no step, or a reason over 500 characters) "
            + "or MALFORMED_REQUEST (unknown step)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: this role cannot update tickets",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND: no ticket with this number",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "DATA_CONFLICT: someone else changed the ticket at the same "
            + "time; reload it and try again",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_STATUS_TRANSITION: the step does not start from the "
            + "ticket's status; STEP_NOT_YOURS: a guide, system or reviewer step",
            content = @Content(schema = @Schema(implementation = ApiError.class), examples = {
                    @ExampleObject(name = "Wrong status", value = "{\"code\":\"INVALID_STATUS_TRANSITION\",\"message\":\"ARCHIVE starts from RESOLVED, but the ticket is TROUBLESHOOTING.\",\"fieldErrors\":{},\"path\":\"/api/tickets/2001/steps\",\"timestamp\":\"2026-11-15T10:00:00Z\"}"),
                    @ExampleObject(name = "Not your step", value = "{\"code\":\"STEP_NOT_YOURS\",\"message\":\"HAND_OVER can only be taken by SYSTEM.\",\"fieldErrors\":{},\"path\":\"/api/tickets/2001/steps\",\"timestamp\":\"2026-11-15T10:00:00Z\"}")}))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = "Pick up ticket", value = "{\"step\": \"PICK_UP_TICKET\"}"),
            @ExampleObject(name = "Request details", value = "{\"step\": \"REQUEST_DETAILS\", \"reason\": \"Asked the customer for a screenshot of the error.\"}"),
            @ExampleObject(name = "Apply fix", value = "{\"step\": \"APPLY_FIX\"}")}))
    TicketDto takeStep(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001")
            @PathVariable Long number,
            @Valid @RequestBody TakeStepRequest request);

    @GetMapping
    @Operation(summary = "My ticket list",
            description = "The tickets assigned to the current agent, newest first. Filter by view and search by ticket "
                    + "number (#2001 or 2001; send # as %23) or CIF. The Home \"My tickets\" badge is totalItems of "
                    + "GET /api/tickets?view=OPEN.")
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Page, counting from 0. Default 0.",
            schema = @Schema(types = {"integer"}, minimum = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Rows per page, 1 to 100; larger values are cut to 100. Default 8.",
            schema = @Schema(types = {"integer"}, minimum = "1", maximum = "100"))
    @Parameter(name = "sort", in = ParameterIn.QUERY, description = "field,direction with field one of createdAt, "
            + "ticketNumber, priority, status or slaDueAt and direction asc or desc, for example createdAt,desc "
            + "(the default) or ticketNumber,asc. Any other field is 400 REQUEST_ERROR.",
            schema = @Schema(types = {"string"}))
    @ApiResponse(responseCode = "200", description = "One page of tickets")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: view is not ALL, OPEN or RESOLVED_CLOSED, page or "
            + "size is not a number, or sort names a field not in the sort list",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: this role cannot open tickets",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<TicketListRow> list(
            @Parameter(description = "ALL (default), OPEN or RESOLVED_CLOSED.") @RequestParam(required = false) TicketListView view,
            @Parameter(description = "Ticket number (#2001 or 2001) or customer CIF.", example = "CIF-100245")
            @RequestParam(required = false) String q,
            @Parameter(hidden = true) @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);

    @PostMapping("/{number}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Log customer communication",
            description = "Adds a note to the Customer contact tab and audits CUSTOMER_CONTACTED.")
    @ApiResponse(responseCode = "201", description = "The logged contact")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: note is empty or too long",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    ContactDto addContact(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001") @PathVariable Long number,
            @Valid @RequestBody ContactRequest request);

    @GetMapping("/{number}/contacts")
    @Operation(summary = "Customer contact tab",
            description = "What the customer was told, newest first, with a count. Every contact is returned (no paging).")
    @ApiResponse(responseCode = "200", description = "Contacts")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    ContactsResponse contacts(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001") @PathVariable Long number);

    @GetMapping("/{number}/timeline")
    @Operation(summary = "Timeline tab",
            description = "Creation and every status change, newest first, read from the audit trail. Every row is "
                    + "returned (no paging). before and after are JSON text, shaped as listed on the Audit tab. Draw a row as \"NEW → TROUBLESHOOTING by mona\" from "
                    + "before.status, after.status and username.")
    @ApiResponse(responseCode = "200", description = "Timeline")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    HistoryResponse timeline(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001") @PathVariable Long number);

    @GetMapping("/{number}/audit")
    @Operation(summary = "Audit tab",
            description = "Every audit record for this ticket, newest first; every row is returned (no paging). The "
                    + "actions, with their before and after JSON:\n"
                    + "- TICKET_CREATED: neither\n"
                    + "- TICKET_STATUS_CHANGED: before {status}, after {status, step, reason}; reason only when given\n"
                    + "- TICKET_TRANSITION_REJECTED: before {status}, after {requestedStatus}\n"
                    + "- CUSTOMER_CONTACTED: after {contactId}, plus source \"GUIDE\" when a guide logged it\n"
                    + "- GUIDE_STARTED: after {guideRunId, guideId, guideTitle, version}\n"
                    + "- GUIDE_COMPLETED: after is the session summary {guideRunId, guideId, guideTitle, version, "
                    + "path, durationSeconds, outcome, outcomeType}\n"
                    + "- SLA_WARNING, SLA_BREACHED: by the system (userId null), after {percentUsed}\n\n"
                    + "Each row also carries the actor's endpoint, httpMethod and ipAddress, as recorded.")
    @ApiResponse(responseCode = "200", description = "Audit records")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    HistoryResponse audit(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001") @PathVariable Long number);
}
