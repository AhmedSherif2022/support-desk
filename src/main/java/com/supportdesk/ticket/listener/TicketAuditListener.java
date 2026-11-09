package com.supportdesk.ticket.listener;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.ticket.event.SlaAtRiskEvent;
import com.supportdesk.ticket.event.SlaBreachedEvent;
import com.supportdesk.ticket.event.TicketCreatedEvent;
import com.supportdesk.ticket.event.TicketStatusChangedEvent;
import com.supportdesk.ticket.event.TicketTransitionRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Turns ticket and SLA events into audit records (E3-S2, E3-S3). Listeners run inside the publisher's
 * transaction, so each record commits with the change it describes. A ticket's audit entity id is its number.
 */
@Component
@RequiredArgsConstructor
public class TicketAuditListener
{
    public static final String ENTITY = "TICKET";

    private final AuditService auditService;

    @EventListener
    public void onCreated(TicketCreatedEvent event)
    {
        auditService.recordForCurrentUser(AuditAction.TICKET_CREATED, ENTITY, id(event.ticketNumber()), null, null);
    }

    @EventListener
    public void onStatusChanged(TicketStatusChangedEvent event)
    {
        auditService.recordForCurrentUser(AuditAction.TICKET_STATUS_CHANGED, ENTITY, id(event.ticketNumber()),
                Map.of("status", event.from()), Map.of("status", event.to()));
    }

    // The rejected change rolls back with its 422, so the rejection is written in a transaction of its own.
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransitionRejected(TicketTransitionRejectedEvent event)
    {
        auditService.recordForCurrentUser(AuditAction.TICKET_TRANSITION_REJECTED, ENTITY, id(event.ticketNumber()),
                Map.of("status", event.from()), Map.of("requestedStatus", event.to()));
    }

    @EventListener
    public void onSlaAtRisk(SlaAtRiskEvent event)
    {
        auditService.record(AuditAction.SLA_WARNING, null, ENTITY, id(event.ticketNumber()), null,
                Map.of("percentUsed", event.percentUsed()));
    }

    @EventListener
    public void onSlaBreached(SlaBreachedEvent event)
    {
        auditService.record(AuditAction.SLA_BREACHED, null, ENTITY, id(event.ticketNumber()), null,
                Map.of("percentUsed", event.percentUsed()));
    }

    private static String id(Long ticketNumber)
    {
        return String.valueOf(ticketNumber);
    }
}
