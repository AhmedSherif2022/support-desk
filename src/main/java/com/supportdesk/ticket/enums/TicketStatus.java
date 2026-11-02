package com.supportdesk.ticket.enums;
import com.supportdesk.ticket.entity.Ticket;

/**
 * Where a ticket is in its life. TicketStatusMachine decides which moves are allowed.
 * Stored as text ("NEW") because Ticket uses @Enumerated(EnumType.STRING).
 */
public enum TicketStatus
{
    NEW,
    TROUBLESHOOTING,
    WAITING_ON_CUSTOMER,
    ESCALATED,
    WITH_SRE,
    SRE_FIXED,
    RESOLVED,
    REOPENED,
    CLOSED;

    /** Open = still needs work (shown in queues and "Open" filters). */
    public boolean isOpen()
    {
        return this != RESOLVED && this != CLOSED;
    }
}
