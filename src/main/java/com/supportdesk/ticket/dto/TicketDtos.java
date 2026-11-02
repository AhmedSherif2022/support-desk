package com.supportdesk.ticket.dto;

import com.supportdesk.ticket.enums.NextAction;

import com.supportdesk.customer.enums.Platform;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
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
            @Schema(description = "Sub-category code (e.g. OTP_NOT_RECEIVED, LOGIN_FAILED, TRANSFER_FAILED, PAYMENT_FAILED, APP_CRASHING).",
                    example = "OTP_NOT_RECEIVED")
            @NotBlank(message = "Choose a category.") String categoryCode,   // a sub-category code
            @Schema(description = "Required (E3-S1). The precheck and the form start from the category default.", example = "HIGH")
            @NotNull(message = "Choose the priority.") TicketPriority priority,                                          // empty = the category's default
            @Schema(description = "Optional short title; omit to use the sub-category name.", example = "OTP SMS delayed")
            @Size(max = 150) String title,                                    // empty = the category name
            @Schema(description = "Problem details reported on the call.", example = "Customer tried signing in twice and did not receive the OTP SMS.")
            @NotBlank(message = "Describe the problem.") @Size(max = 2000) String description,
            @Schema(description = "Transaction reference (TRX-YYYY-NNNNN) when required by the category.", nullable = true)
            @Size(max = 40) String reference,                                 // TRX-... when the category needs it
            @Schema(description = "Optional. Omit to attach the customer's latest failed error code on this service, "
                    + "or else the category default (E3-S1).", nullable = true)
            @Size(max = 40) String errorCode,                                 // empty = the category's default code
            @Schema(example = "MOBILE")
            @NotNull(message = "Choose the platform.") Platform platform,
            @Schema(description = "Set true to create even when an open ticket already exists for the same customer and category.", example = "false")
            boolean allowDuplicate)                                           // true = create even if one is open
    {
    }


    public record PrecheckRequest(
            @Schema(example = "CIF-100245")
            @NotBlank String cif,
            @Schema(example = "PAYMENT_FAILED")
            @NotBlank String categoryCode)
    {
    }


    public record ChangeStatusRequest(
            @Schema(description = "Target agent-selectable status (e.g. TROUBLESHOOTING, WAITING_ON_CUSTOMER, RESOLVED, REOPENED, CLOSED).",
                    example = "TROUBLESHOOTING")
            @NotNull(message = "Choose the new status.") TicketStatus status,
            @Schema(example = "Investigating declined transaction logs.")
            @Size(max = 500) String reason)
    {
    }


    public record PrecheckDto(List<Warning> warnings)
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
            String description,
            @Schema(example = "CIF-100245") String customerCif,
            @Schema(example = "Sara Mahmoud") String customerName,
            @Schema(example = "Payments / Payment Failed") String category,
            @Schema(example = "PAYMENT_FAILED") String categoryCode,
            @Schema(example = "HIGH") TicketPriority priority,
            @Schema(example = "TROUBLESHOOTING") TicketStatus status,
            @Schema(example = "MANUAL") TicketOrigin origin,
            @Schema(example = "MOBILE") Platform platform,
            String errorCode,
            @Schema(example = "TRX-2026-01111") String reference,
            @Schema(example = "Mona") String assignedAgent,
            @Schema(example = "AT_RISK") SlaState slaState,
            Instant slaDueAt,
            Set<TicketStatus> allowedNextStatuses,
            int reopenCount,
            Instant createdAt,
            @Schema(description = "SLA meter: percent of the target used.", example = "81") int slaPercentUsed,
            @Schema(description = "The one SLA value: time left, Paused, Met, or Breached Xh Ym.", example = "1h 30m left")
            String slaLabel,
            @Schema(description = "The pinned next step (E3-S5).", example = "ESCALATE") NextAction nextAction,
            @Schema(example = "Escalate") String nextActionLabel)
    {
    }
}
