package com.supportdesk.ticket.enums;
import com.supportdesk.ticket.entity.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Where a ticket is in its life. TicketStep lists every allowed move (the agreed ticket diagram);
 * TicketStatusMachine guards them.
 * Stored as text ("NEW") because Ticket uses @Enumerated(EnumType.STRING).
 */
@Schema(enumAsRef = true, description = "Where a ticket is. It changes only by a step (see TicketStep). The SRE "
        + "statuses move by system steps from the Azure DevOps integration (Epic 5, not built yet); until then a "
        + "ticket stays in ESCALATED or REOPENED.\n"
        + "- NEW: just created\n"
        + "- TROUBLESHOOTING: the agent (or a guide) is working on it\n"
        + "- WAITING_ON_CUSTOMER: waiting for details from the customer; the SLA clock is paused\n"
        + "- ESCALATED: handed to SRE; the system step SRE_ACCEPTS moves it on\n"
        + "- WITH_SRE: SRE is working on it; the system step DEPLOY_FIX moves it on\n"
        + "- SRE_FIXED: SRE deployed a fix; the system step SEND_TO_REVIEW moves it to review\n"
        + "- IN_REVIEW: a Reviewer checks the SRE fix\n"
        + "- RESOLVED: fixed; the SLA clock stops\n"
        + "- REOPENED: the Reviewer rejected the SRE fix; the system step FIX_FAILED sends it back to SRE\n"
        + "- CLOSED: archived; final")
public enum TicketStatus
{
    NEW,
    TROUBLESHOOTING,
    WAITING_ON_CUSTOMER,
    ESCALATED,
    WITH_SRE,
    SRE_FIXED,
    IN_REVIEW,
    RESOLVED,
    REOPENED,
    CLOSED;

    /** Open = still needs work (shown in queues and "Open" filters). */
    public boolean isOpen()
    {
        return this != RESOLVED && this != CLOSED;
    }
}
