package com.supportdesk.ticket.service;
import com.supportdesk.ticket.dto.SlaSnapshot;

import com.supportdesk.ticket.config.TicketProperties;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Reads the SLA clock: the target for the ticket's priority, time spent paused, and the percent used.
 * The clock is the injected one, so tests can move it.
 */
@Service
@RequiredArgsConstructor
public class SlaService
{
    private final TicketProperties properties;
    private final Clock clock;

    public Instant now()
    {
        return clock.instant();
    }

    public Duration targetFor(TicketPriority priority)
    {
        return properties.sla().targetFor(priority);
    }

    public Instant dueAt(TicketPriority priority, Instant startedAt)
    {
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        return startedAt.plus(targetFor(priority));
    }

    public SlaSnapshot evaluate(Ticket ticket)
    {
        return evaluate(ticket, clock.instant());
    }

    public SlaSnapshot evaluate(Ticket ticket, Instant now)
    {
        Objects.requireNonNull(ticket, "ticket must not be null");
        Objects.requireNonNull(now, "now must not be null");

        Duration target = targetFor(ticket.getPriority());
        Instant end = effectiveEnd(ticket, now);
        Duration remaining = Duration.between(end, ticket.getSlaDueAt());
        Duration elapsed = target.minus(remaining);
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
            remaining = target;
        }
        int percent = percentUsed(elapsed, target);
        return new SlaSnapshot(stateFor(ticket, percent), percent, elapsed, target, remaining, ticket.isSlaPaused());
    }

    public void pause(Ticket ticket)
    {
        Objects.requireNonNull(ticket, "ticket must not be null");
        Instant now = clock.instant();
        ticket.pauseSla(now);
        ticket.updateSlaState(evaluate(ticket, now).state());
    }

    public void resume(Ticket ticket)
    {
        Objects.requireNonNull(ticket, "ticket must not be null");
        Instant now = clock.instant();
        ticket.resumeSla(now);
        ticket.updateSlaState(evaluate(ticket, now).state());
    }

    public void resolve(Ticket ticket)
    {
        Objects.requireNonNull(ticket, "ticket must not be null");
        Instant now = clock.instant();
        ticket.moveTo(TicketStatus.RESOLVED, now);
        ticket.updateSlaState(evaluate(ticket, now).state());
    }

    private Instant effectiveEnd(Ticket ticket, Instant now)
    {
        if (ticket.isSlaPaused()) {
            return ticket.getSlaPausedAt();
        }
        if (!ticket.isOpen()) {
            if (ticket.getResolvedAt() != null) {
                return ticket.getResolvedAt();
            }
            if (ticket.getClosedAt() != null) {
                return ticket.getClosedAt();
            }
        }
        return now;
    }

    private int percentUsed(Duration elapsed, Duration target)
    {
        long percent = elapsed.toMillis() * 100 / target.toMillis();
        if (percent > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) percent;
    }

    private SlaState stateFor(Ticket ticket, int percent)
    {
        if (!ticket.isOpen()) {
            return percent >= 100 ? SlaState.BREACHED : SlaState.MET;
        }
        if (percent >= 100) {
            return SlaState.BREACHED;
        }
        if (ticket.isSlaPaused()) {
            return SlaState.PAUSED;
        }
        if (percent >= properties.sla().warningAtPercent()) {
            return SlaState.AT_RISK;
        }
        return SlaState.ON_TRACK;
    }
}
