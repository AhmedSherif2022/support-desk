package com.supportdesk.ticket.event;
import com.supportdesk.ticket.entity.TicketStatus;
public final class TicketEvents
{
    private TicketEvents()
    {
    }

    public record TicketCreatedEvent(Long ticketId, Long ticketNumber, Long customerId, Long agentId)
    {
    }


    public record TicketStatusChangedEvent(Long ticketId, Long ticketNumber,
                                           TicketStatus from, TicketStatus to, Long agentId)
    {
    }


    public record TicketTransitionRejectedEvent(Long ticketId, Long ticketNumber,
                                                TicketStatus from, TicketStatus to, Long agentId)
    {
    }
}
