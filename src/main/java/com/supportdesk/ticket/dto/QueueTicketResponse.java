package com.supportdesk.ticket.dto;

import com.supportdesk.customer.enums.Platform;
import com.supportdesk.customer.enums.Segment;
import com.supportdesk.ticket.enums.NextAction;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "One open ticket row in the agent home queue (E3-S4): what it is, who it is for, its SLA, and the one next step.")
public record QueueTicketResponse(
        @Schema(example = "1")
        Long id,
        @Schema(description = "Display ticket number.", example = "#2001")
        String number,
        @Schema(description = "The issue.", example = "Card payment declined")
        String title,
        @Schema(example = "CIF-100245")
        String customerCif,
        @Schema(example = "Sara Mahmoud")
        String customerName,
        @Schema(example = "PREMIUM")
        Segment segment,
        @Schema(description = "Parent / sub-category path.", example = "Payments / Payment Failed")
        String category,
        @Schema(example = "MOBILE")
        Platform platform,
        @Schema(example = "ERR_AUTH_503")
        String errorCode,
        @Schema(description = "Manual, Auto-created or From incident.", example = "MANUAL")
        TicketOrigin origin,
        @Schema(example = "HIGH")
        TicketPriority priority,
        @Schema(example = "TROUBLESHOOTING")
        TicketStatus status,
        @Schema(example = "Mona")
        String assignedAgent,
        @Schema(example = "AT_RISK")
        SlaState slaState,
        @Schema(description = "The one SLA value: time left, Paused, Met, or Breached Xh Ym.", example = "1h 30m left")
        String slaLabel,
        @Schema(description = "Current SLA deadline (shifted forward by any completed pauses).")
        Instant slaDueAt,
        @Schema(description = "Percent of the SLA target already used. Over 100 means breached.", example = "81")
        int percentUsed,
        @Schema(description = "Seconds left on the target. Negative once the target has passed.", example = "5400")
        long remainingSeconds,
        @Schema(description = "True while the SLA clock is stopped (WAITING_ON_CUSTOMER).", example = "false")
        boolean paused,
        @Schema(example = "ESCALATE")
        NextAction nextAction,
        @Schema(example = "Escalate")
        String nextActionLabel)
{
}
