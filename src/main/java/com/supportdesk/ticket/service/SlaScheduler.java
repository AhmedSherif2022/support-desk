package com.supportdesk.ticket.service;
import com.supportdesk.ticket.dto.SlaSnapshot;

import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.event.SlaAtRiskEvent;
import com.supportdesk.ticket.event.SlaBreachedEvent;
import com.supportdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Once a minute, moves open tickets across the warning and breach lines and publishes each crossing once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaScheduler
{
    private static final List<TicketStatus> CLOSED_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository tickets;
    private final SlaService slaService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
    @Transactional
    public void refreshOpenTickets()
    {
        Instant now = clock.instant();
        for (Ticket ticket : tickets.findByStatusNotIn(CLOSED_STATUSES)) {
            SlaState previous = ticket.getSlaState();
            SlaSnapshot snapshot = slaService.evaluate(ticket, now);
            if (!ticket.updateSlaState(snapshot.state())) {
                continue;
            }
            tickets.save(ticket);
            publishCrossing(ticket, previous, snapshot, now);
        }
    }

    private void publishCrossing(Ticket ticket, SlaState previous, SlaSnapshot snapshot, Instant now)
    {
        if (snapshot.state() == SlaState.BREACHED && previous != SlaState.BREACHED) {
            events.publishEvent(new SlaBreachedEvent(
                    ticket.getId(), ticket.getTicketNumber(), snapshot.percentUsed(), now));
            log.info("SLA breached: ticket={}, percent={}", ticket.displayNumber(), snapshot.percentUsed());
            return;
        }
        if (snapshot.state() == SlaState.AT_RISK && previous == SlaState.ON_TRACK) {
            events.publishEvent(new SlaAtRiskEvent(
                    ticket.getId(), ticket.getTicketNumber(), snapshot.percentUsed(), now));
            log.info("SLA at risk: ticket={}, percent={}", ticket.displayNumber(), snapshot.percentUsed());
        }
    }
}
