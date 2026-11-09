package com.supportdesk.ticket.event;

import com.supportdesk.ticket.enums.TicketStatus;

/**
 * Published when a ticket moves to another status (audit action: TICKET_STATUS_CHANGED).
 */
public record TicketStatusChangedEvent(Long ticketId, Long ticketNumber, TicketStatus from, TicketStatus to, Long agentId)
{
}
