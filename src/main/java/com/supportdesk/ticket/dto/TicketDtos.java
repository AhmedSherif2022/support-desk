package com.supportdesk.ticket.dto;

import com.supportdesk.ticket.enums.NextAction;

import com.supportdesk.customer.enums.Platform;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.enums.TicketStep;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;


public final class TicketDtos
{
    private TicketDtos()
    {
    }

    public record CreateTicketRequest(
            @Schema(description = "Verified customer CIF.", example = "CIF-100245")
            @NotBlank(message = "Choose the customer.") String cif,
            @Schema(description = "A sub-category code from GET /api/tickets/categories (a row with a parentCode).",
                    example = "OTP_NOT_RECEIVED")
            @NotBlank(message = "Choose a category.") String categoryCode,   // a sub-category code
            @Schema(description = "Required. Start the form from the category's defaultPriority (GET /api/tickets/categories).", example = "HIGH")
            @NotNull(message = "Choose the priority.") TicketPriority priority,
            @Schema(description = "Optional short title; omit to use the sub-category name.", example = "OTP SMS delayed")
            @Size(max = 150) String title,                                    // empty = the category name
            @Schema(description = "Problem details reported on the call.", example = "Customer tried signing in twice and did not receive the OTP SMS.")
            @NotBlank(message = "Describe the problem.") @Size(max = 2000) String description,
            @Schema(description = "Transaction reference (TRX-YYYY-NNNNN) when the category's referenceRequired is true; "
                    + "its referenceHint says how to write it.", nullable = true)
            @Size(max = 40) String reference,                                 // TRX-... when the category needs it
            @Schema(description = "Optional. Omit to attach the customer's latest failed error code on this service, "
                    + "or else the category default.", nullable = true)
            @Size(max = 40) String errorCode,                                 // empty = the category's default code
            @Schema(example = "MOBILE")
            @NotNull(message = "Choose the platform.") Platform platform,
            @Schema(description = "Set true to create even when an open ticket already exists for the same customer and category.", example = "false")
            boolean allowDuplicate)                                           // true = create even if one is open
    {
    }


    public record CategoryDto(
            @Schema(example = "PAYMENT_FAILED") String code,
            @Schema(example = "Payment Failed") String name,
            @Schema(description = "The parent category's code; null for a top-level category. A ticket uses a "
                    + "sub-category: a row with a parentCode.", example = "PAYMENTS", types = {"string", "null"})
            String parentCode,
            @Schema(description = "Start the create form's priority from this.", example = "HIGH") TicketPriority defaultPriority,
            @Schema(description = "True when a ticket in this category needs a transaction reference.", example = "true")
            boolean referenceRequired,
            @Schema(description = "How to write the reference; null when none is needed.",
                    example = "Use the format TRX-2026-01111", types = {"string", "null"}) String referenceHint)
    {
    }


    public record PrecheckRequest(
            @Schema(example = "CIF-100245")
            @NotBlank String cif,
            @Schema(example = "PAYMENT_FAILED")
            @NotBlank String categoryCode)
    {
    }


    public record TakeStepRequest(
            @Schema(description = "One of the ticket's availableSteps.", example = "PICK_UP_TICKET")
            @NotNull(message = "Choose the step.") TicketStep step,
            @Schema(description = "Why. Required for REVIEWER_REJECTS (blank counts as missing); saved trimmed "
                    + "in the audit trail.",
                    example = "Login still fails after the deploy.")
            @Size(max = 500) String reason)
    {
    }


    public record PrecheckDto(@Schema(description = "Things to check before creating the ticket; empty when there are none.") List<Warning> warnings)
    {

        public record Warning(
                @Schema(example = "DUPLICATE_OPEN_TICKET") String code,
                @Schema(example = "Ticket #2001 is already open for this problem.") String message,
                @Schema(example = "#2001") String existingTicketNumber)
        {
        }
    }

    public record TicketDto(
            @Schema(example = "1") Long id,
            @Schema(example = "#2001") String number,
            @Schema(example = "Card payment declined") String title,
            @Schema(description = "What the customer reported.", example = "Card payment fails at checkout.") String description,
            @Schema(description = "The customer's CIF. Null for a Reviewer: Guide Admin roles never see customer data.",
                    example = "CIF-100245", types = {"string", "null"}) String customerCif,
            @Schema(description = "The customer's name. Null for a Reviewer.",
                    example = "Sara Mahmoud", types = {"string", "null"}) String customerName,
            @Schema(example = "Payments / Payment Failed") String category,
            @Schema(example = "PAYMENT_FAILED") String categoryCode,
            @Schema(example = "HIGH") TicketPriority priority,
            @Schema(description = "Current status; see TicketStatus.", example = "TROUBLESHOOTING") TicketStatus status,
            @Schema(example = "MANUAL") TicketOrigin origin,
            @Schema(example = "MOBILE") Platform platform,
            @Schema(description = "The customer's latest error code for this service, or the category's default. Null when neither exists.", example = "PAY_TIMEOUT") String errorCode,
            @Schema(example = "TRX-2026-01111") String reference,
            @Schema(example = "Mona") String assignedAgent,
            @Schema(example = "AT_RISK") SlaState slaState,
            @Schema(description = "When the SLA target runs out. Time spent paused moves it later.", example = "2026-11-16T17:30:00Z") Instant slaDueAt,
            @Schema(description = "Steps the current user can take on the ticket now: its action buttons. "
                    + "In Review, both the agent and the reviewer get REVIEWER_APPROVES and REVIEWER_REJECTS. "
                    + "Empty when only someone else can move it (a guide or the system).",
                    example = "[\"REQUEST_DETAILS\", \"APPLY_FIX\"]")
            List<TicketStep> availableSteps,
            @Schema(description = "Times the SRE fix was rejected in review (each move into REOPENED).", example = "0")
            int reopenCount,
            @Schema(example = "2026-11-16T09:30:00Z") Instant createdAt,
            @Schema(description = "SLA meter: percent of the target used.", example = "81") int slaPercentUsed,
            @Schema(description = "The SLA meter's text: time left, Paused, Met, or Breached Xh Ym.", example = "1h 30m left")
            String slaLabel,
            @Schema(description = "The pinned next action, a hint rather than a step; see NextAction for the value "
                    + "per status and what to call. Null when the ticket is CLOSED.",
                    example = "ESCALATE") NextAction nextAction,
            @Schema(description = "Display text for nextAction; null when nextAction is null.", example = "Escalate",
                    types = {"string", "null"}) String nextActionLabel,
            @Schema(description = "The unfinished guide run on this ticket: reopen it after a reload with "
                    + "GET /api/guide-runs/{id}. Null when there is none, or when the ticket is not Troubleshooting "
                    + "(starting a guide then abandons the old run).", example = "17", types = {"integer", "null"})
            Long activeGuideRunId)
    {
        /** The same ticket without the customer's CIF and name: what a Reviewer gets (E1-S2). */
        public TicketDto withoutCustomer()
        {
            return new TicketDto(id, number, title, description, null, null, category, categoryCode, priority,
                    status, origin, platform, errorCode, reference, assignedAgent, slaState, slaDueAt,
                    availableSteps, reopenCount, createdAt, slaPercentUsed, slaLabel, nextAction,
                    nextActionLabel, activeGuideRunId);
        }
    }
}
