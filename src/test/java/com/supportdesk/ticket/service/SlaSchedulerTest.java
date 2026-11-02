package com.supportdesk.ticket.service;

import com.supportdesk.customer.enums.Platform;
import com.supportdesk.ticket.config.TicketProperties;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.event.SlaAtRiskEvent;
import com.supportdesk.ticket.event.SlaBreachedEvent;
import com.supportdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaSchedulerTest
{
    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    @Mock
    private TicketRepository tickets;

    @Mock
    private ApplicationEventPublisher events;

    private MutableClock clock;
    private SlaService slaService;
    private SlaScheduler scheduler;

    @BeforeEach
    void setUp()
    {
        clock = new MutableClock(NOW);
        slaService = new SlaService(properties(), clock);
        scheduler = new SlaScheduler(tickets, slaService, events, clock);
    }

    @Test
    void crossingTheWarningLinePublishesOnce()
    {
        Ticket ticket = open(7L, 2007L, TicketPriority.HIGH, NOW.minus(Duration.ofHours(6).plusMinutes(24)));
        when(tickets.findByStatusNotIn(anyCollection())).thenReturn(List.of(ticket));

        scheduler.refreshOpenTickets();
        scheduler.refreshOpenTickets();

        ArgumentCaptor<SlaAtRiskEvent> published = ArgumentCaptor.forClass(SlaAtRiskEvent.class);
        verify(events, times(1)).publishEvent(published.capture());
        assertThat(published.getValue()).isEqualTo(new SlaAtRiskEvent(7L, 2007L, 80, NOW));
        assertThat(ticket.getSlaState()).isEqualTo(SlaState.AT_RISK);
        verify(tickets, times(1)).save(ticket);
    }

    @Test
    void warningTicketCrossingIntoBreachPublishesBreachOnce()
    {
        Ticket ticket = open(9L, 2009L, TicketPriority.CRITICAL, NOW.minus(Duration.ofMinutes(96)));
        when(tickets.findByStatusNotIn(anyCollection())).thenReturn(List.of(ticket));

        scheduler.refreshOpenTickets();
        assertThat(ticket.getSlaState()).isEqualTo(SlaState.AT_RISK);

        clock.advance(Duration.ofMinutes(24));
        scheduler.refreshOpenTickets();
        scheduler.refreshOpenTickets();

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events, times(2)).publishEvent(published.capture());
        assertThat(published.getAllValues().get(0)).isEqualTo(new SlaAtRiskEvent(9L, 2009L, 80, NOW));
        assertThat(published.getAllValues().get(1)).isEqualTo(
                new SlaBreachedEvent(9L, 2009L, 100, NOW.plus(Duration.ofMinutes(24))));
        assertThat(ticket.getSlaState()).isEqualTo(SlaState.BREACHED);
        verify(tickets, times(2)).save(ticket);
    }

    @Test
    void crossingTheTargetPublishesABreachAndSkipsTheWarning()
    {
        Ticket ticket = open(8L, 2008L, TicketPriority.CRITICAL, NOW.minus(Duration.ofMinutes(180)));
        when(tickets.findByStatusNotIn(anyCollection())).thenReturn(List.of(ticket));

        scheduler.refreshOpenTickets();

        ArgumentCaptor<SlaBreachedEvent> published = ArgumentCaptor.forClass(SlaBreachedEvent.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getValue()).isEqualTo(new SlaBreachedEvent(8L, 2008L, 150, NOW));
        assertThat(ticket.getSlaState()).isEqualTo(SlaState.BREACHED);
    }

    @Test
    void pausedTicketDoesNotAdvanceOrPublishEvents()
    {
        Ticket ticket = open(10L, 2010L, TicketPriority.CRITICAL, NOW.minus(Duration.ofHours(3)));
        ticket.pauseSla(NOW.minus(Duration.ofHours(2)));
        ticket.updateSlaState(SlaState.PAUSED);
        when(tickets.findByStatusNotIn(anyCollection())).thenReturn(List.of(ticket));

        scheduler.refreshOpenTickets();

        assertThat(ticket.getSlaState()).isEqualTo(SlaState.PAUSED);
        verify(tickets, never()).save(ticket);
        verifyNoInteractions(events);
    }

    private Ticket open(Long id, Long ticketNumber, TicketPriority priority, Instant startedAt)
    {
        Ticket ticket = new Ticket(ticketNumber, null, null, "Ticket " + ticketNumber, "description",
                priority, TicketOrigin.MANUAL, Platform.MOBILE, null, null, null,
                startedAt, slaService.dueAt(priority, startedAt));
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }

    private static TicketProperties properties()
    {
        return new TicketProperties(new TicketProperties.Sla(80, Map.of(
                TicketPriority.CRITICAL, Duration.ofHours(2),
                TicketPriority.HIGH, Duration.ofHours(8),
                TicketPriority.MEDIUM, Duration.ofHours(24),
                TicketPriority.LOW, Duration.ofHours(72))));
    }

    private static final class MutableClock extends Clock
    {
        private Instant instant;

        private MutableClock(Instant instant)
        {
            this.instant = instant;
        }

        private void advance(Duration duration)
        {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return instant;
        }
    }
}
