package com.supportdesk.ticket.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The one next step a ticket asks for (E3-S5). Shared by the queue rows and the ticket's pinned bar,
 * so both always name the same action.
 */
@Schema(enumAsRef = true, description = "The next action the ticket asks for, by status, and what the frontend calls for it. "
        + "A value with nothing to call is shown as information, not as a button.\n"
        + "- NEW: START_GUIDE when a guide fits (pick one from `GET /api/guides/recommended`, then call `POST /api/guide-runs`), otherwise PICK_UP_TICKET (take step PICK_UP_TICKET)\n"
        + "- TROUBLESHOOTING: ESCALATE, with nothing to call yet (the system step HAND_OVER comes with Epic 5). The agent "
        + "can still take the ticket's availableSteps (REQUEST_DETAILS, APPLY_FIX) or run a guide\n"
        + "- WAITING_ON_CUSTOMER: RESUME (take step DETAILS_RECEIVED)\n"
        + "- ESCALATED, REOPENED: WAIT_FOR_SRE (nothing to call)\n"
        + "- WITH_SRE: WAIT_FOR_SRE, or ASK_SRE_FOR_UPDATE once the SLA is at risk or breached (nothing to call yet)\n"
        + "- SRE_FIXED, IN_REVIEW: WAIT_FOR_REVIEW (nothing to call; a Reviewer decides)\n"
        + "- RESOLVED: ARCHIVE (take step ARCHIVE)\n"
        + "- CLOSED: none (null)")
public enum NextAction
{
    START_GUIDE("Start guide"),
    PICK_UP_TICKET("Pick up ticket"),
    ESCALATE("Escalate"),
    RESUME("Resume"),
    WAIT_FOR_SRE("Wait for SRE"),
    ASK_SRE_FOR_UPDATE("Ask SRE for update"),
    WAIT_FOR_REVIEW("Wait for review"),
    ARCHIVE("Archive");

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
    public static NextAction resolve(TicketStatus status, SlaState sla, boolean guideAvailable)
    {
        return switch (status) {
            case NEW -> guideAvailable ? START_GUIDE : PICK_UP_TICKET;
            case TROUBLESHOOTING -> ESCALATE;
            case WAITING_ON_CUSTOMER -> RESUME;
            // A rejected fix (Reopened) goes back to SRE: Fix failed.
            case ESCALATED, REOPENED -> WAIT_FOR_SRE;
            // Waiting on SRE is fine until the SLA is in danger; then the agent chases them.
            case WITH_SRE -> sla == SlaState.AT_RISK || sla == SlaState.BREACHED ? ASK_SRE_FOR_UPDATE : WAIT_FOR_SRE;
            case SRE_FIXED, IN_REVIEW -> WAIT_FOR_REVIEW;
            case RESOLVED -> ARCHIVE;
            case CLOSED -> null;
        };
    }
}
