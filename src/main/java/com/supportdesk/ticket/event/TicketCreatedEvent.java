package com.supportdesk.ticket.event;

/**
 * Published when a ticket is created (audit action: TICKET_CREATED).
 */
public record TicketCreatedEvent(Long ticketId, Long ticketNumber, Long customerId, Long agentId)
{
}
