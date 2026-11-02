package com.supportdesk.ticket.controller;

import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.exception.ApiError;
import com.supportdesk.ticket.enums.TicketListView;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactDto;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactRequest;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactsResponse;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.HistoryResponse;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.TicketListRow;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.supportdesk.ticket.enums.QueueChip;
import com.supportdesk.ticket.dto.QueueResponse;
import com.supportdesk.ticket.dto.TicketDtos.ChangeStatusRequest;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckDto;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

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
            description = "Open tickets: \"confirm fix\" first, paused last, and in between the highest SLA % used first. "
                    + "Each row names one next action. Counts stay for every chip even when one chip is selected.")
    @ApiResponse(responseCode = "200", description = "The open queue and chip counts")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: chip is not ALL, AT_RISK, CONFIRM_FIX, or AUTO_CREATED",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: this role cannot open the queue",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    QueueResponse queue(
            @Parameter(description = "ALL, AT_RISK (at risk or breached), CONFIRM_FIX or AUTO_CREATED. Omitted or empty means ALL.", example = "ALL")
            @RequestParam(required = false) QueueChip chip);

    @GetMapping("/{number}")
    @Operation(summary = "Get ticket by number",
            description = "Returns the ticket details, SLA state, and allowed next agent statuses.")
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

    @PatchMapping("/{number}/status")
    @Operation(summary = "Change ticket status",
            description = "Moves the ticket to an allowed agent status, pauses/resumes/freezes its SLA clock, and recalculates slaState.")
    @ApiResponse(responseCode = "200", description = "Updated ticket")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or MALFORMED_REQUEST",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: no token, or it expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: this role cannot update tickets",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND: no ticket with this number",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "INVALID_STATUS_TRANSITION or STATUS_SET_BY_SYSTEM",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    TicketDto changeStatus(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001")
            @PathVariable Long number,
            @Valid @RequestBody ChangeStatusRequest request);

    @GetMapping
    @Operation(summary = "My ticket list (E3-S7)",
            description = "The current agent's tickets, newest first. Filter by view and search by ticket number (#2001) or CIF. "
                    + "Paginated: 8 per page by default (page from 0).")
    @ApiResponse(responseCode = "200", description = "One page of tickets")
    @ApiResponse(responseCode = "400", description = "REQUEST_ERROR: view is not ALL, OPEN or RESOLVED_CLOSED",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: this role cannot open tickets",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<TicketListRow> list(
            @Parameter(description = "ALL (default), OPEN or RESOLVED_CLOSED.") @RequestParam(required = false) TicketListView view,
            @Parameter(description = "Ticket number (#2001 or 2001) or customer CIF.", example = "CIF-100245")
            @RequestParam(required = false) String q,
            @ParameterObject @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);

    @PostMapping("/{number}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Log customer communication (E3-S6)",
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
    @Operation(summary = "Customer contact tab (E3-S5)", description = "What the customer was told, newest first, with a count.")
    @ApiResponse(responseCode = "200", description = "Contacts")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    ContactsResponse contacts(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001") @PathVariable Long number);

    @GetMapping("/{number}/timeline")
    @Operation(summary = "Timeline tab (E3-S5)",
            description = "Creation and every status change, newest first, with before and after. Read from the audit trail.")
    @ApiResponse(responseCode = "200", description = "Timeline")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    HistoryResponse timeline(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001") @PathVariable Long number);

    @GetMapping("/{number}/audit")
    @Operation(summary = "Audit tab (E3-S5)",
            description = "Every audit record for this ticket (created, status changes, rejected changes, contacts, SLA warnings), newest first.")
    @ApiResponse(responseCode = "200", description = "Audit records")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "TICKET_NOT_FOUND", content = @Content(schema = @Schema(implementation = ApiError.class)))
    HistoryResponse audit(
            @Parameter(description = "Ticket number without the # prefix.", example = "2001") @PathVariable Long number);
}
