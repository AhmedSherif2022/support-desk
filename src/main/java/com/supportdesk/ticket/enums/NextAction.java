package com.supportdesk.ticket.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The one next step a ticket asks for (E3-S5). Shared by the queue rows and the ticket's pinned bar,
 * so both always name the same action.
 */
@Schema(description = "The single next step for the ticket.")
public enum NextAction
{
    START_GUIDE("Start guide"),
    ESCALATE("Escalate"),
    RESUME("Resume"),
    WAIT_FOR_SRE("Wait for SRE"),
    ASK_SRE_FOR_UPDATE("Ask SRE for update"),
    CALL_AND_CONFIRM("Call & confirm"),
    CLOSE("Close");

    private final String label;

    NextAction(String label)
    {
        this.label = label;
    }

    public String label()
    {
        return label;
    }

    /** Null for a closed ticket: nothing is left to do. */
    public static NextAction resolve(TicketStatus status, SlaState sla)
    {
        return switch (status) {
            case NEW, REOPENED -> START_GUIDE;
            case TROUBLESHOOTING -> ESCALATE;
            case WAITING_ON_CUSTOMER -> RESUME;
            case ESCALATED -> WAIT_FOR_SRE;
            // Waiting on SRE is fine until the SLA is in danger; then the agent chases them.
            case WITH_SRE -> sla == SlaState.AT_RISK || sla == SlaState.BREACHED ? ASK_SRE_FOR_UPDATE : WAIT_FOR_SRE;
            case SRE_FIXED -> CALL_AND_CONFIRM;
            case RESOLVED -> CLOSE;
            case CLOSED -> null;
        };
    }
}
