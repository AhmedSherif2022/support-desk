package com.supportdesk.ticket.event;

import com.supportdesk.ticket.enums.TicketStatus;

/**
 * Published when a status move is not allowed (audit action: TICKET_TRANSITION_REJECTED).
 */
public record TicketTransitionRejectedEvent(Long ticketId, Long ticketNumber, TicketStatus from, TicketStatus to, Long agentId)
{
}
