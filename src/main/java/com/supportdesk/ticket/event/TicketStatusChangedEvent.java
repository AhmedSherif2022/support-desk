package com.supportdesk.ticket.event;

import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.enums.TicketStep;

/**
 * Published when a ticket takes a step to another status (audit action: TICKET_STATUS_CHANGED).
 * reason is null unless the step was given one.
 */
public record TicketStatusChangedEvent(Long ticketId, Long ticketNumber, TicketStatus from, TicketStatus to,
                                       TicketStep step, String reason, Long agentId)
{
}
