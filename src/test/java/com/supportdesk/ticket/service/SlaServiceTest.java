package com.supportdesk.ticket.service;
import com.supportdesk.ticket.dto.SlaSnapshot;

import com.supportdesk.customer.enums.Platform;
import com.supportdesk.ticket.config.TicketProperties;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlaServiceTest
{
    private static final Instant START = Instant.parse("2026-11-01T09:00:00Z");

    private MutableClock clock;
    private SlaService sla;

    @BeforeEach
    void setUp()
    {
        clock = new MutableClock(START);
        sla = new SlaService(properties(), clock);
    }

    @Test
    void freshTicketIsOnTrackAtZero()
    {
        Ticket ticket = open(2001L, "New", TicketPriority.HIGH, START);

        SlaSnapshot snapshot = sla.evaluate(ticket);

        assertThat(snapshot.state()).isEqualTo(SlaState.ON_TRACK);
        assertThat(snapshot.percentUsed()).isZero();
        assertThat(snapshot.paused()).isFalse();
    }

    @Test
    void warningStartsAtTheConfiguredPercent()
    {
        // HIGH target is 8h (480m), so 80% is 6h24m (384m).
        Ticket ticket = open(2001L, "Card", TicketPriority.HIGH, START);
        clock.advance(Duration.ofHours(6).plusMinutes(23));

        assertThat(sla.evaluate(ticket).state()).isEqualTo(SlaState.ON_TRACK);
        assertThat(sla.evaluate(ticket).percentUsed()).isEqualTo(79);

        clock.advance(Duration.ofMinutes(1));

        SlaSnapshot warning = sla.evaluate(ticket);
        assertThat(warning.state()).isEqualTo(SlaState.AT_RISK);
        assertThat(warning.percentUsed()).isEqualTo(80);
    }

    @Test
    void breachStartsWhenTheTargetIsUsedUp()
    {
        Ticket ticket = open(2001L, "Card", TicketPriority.HIGH, START);
        clock.advance(Duration.ofHours(8));

        SlaSnapshot snapshot = sla.evaluate(ticket);

        assertThat(snapshot.state()).isEqualTo(SlaState.BREACHED);
        assertThat(snapshot.percentUsed()).isEqualTo(100);
        assertThat(snapshot.remaining()).isZero();
    }

    @Test
    void theSameElapsedTimeIsSafeOnALongerTarget()
    {
        clock.advance(Duration.ofHours(6).plusMinutes(24));
        Ticket low = open(2002L, "Statement", TicketPriority.LOW, START);

        SlaSnapshot snapshot = sla.evaluate(low);

        assertThat(snapshot.state()).isEqualTo(SlaState.ON_TRACK);
        assertThat(snapshot.percentUsed()).isLessThan(80);
    }

    @Test
    void pauseStopsTheClockUntilResume()
    {
        // CRITICAL target is 2h (120m).
        Ticket ticket = open(2001L, "Transfer", TicketPriority.CRITICAL, START);
        clock.advance(Duration.ofMinutes(60));
        sla.pause(ticket);

        clock.advance(Duration.ofHours(2));
        SlaSnapshot frozen = sla.evaluate(ticket);
        assertThat(frozen.paused()).isTrue();
        assertThat(frozen.percentUsed()).isEqualTo(50);
        assertThat(frozen.state()).isEqualTo(SlaState.PAUSED);

        sla.resume(ticket);
        assertThat(sla.evaluate(ticket).state()).isEqualTo(SlaState.ON_TRACK);

        clock.advance(Duration.ofMinutes(36));
        assertThat(sla.evaluate(ticket).state()).isEqualTo(SlaState.AT_RISK);
        assertThat(sla.evaluate(ticket).percentUsed()).isEqualTo(80);

        clock.advance(Duration.ofMinutes(24));
        assertThat(sla.evaluate(ticket).state()).isEqualTo(SlaState.BREACHED);
        assertThat(sla.evaluate(ticket).percentUsed()).isEqualTo(100);
    }

    @Test
    void multiplePausesAccumulateWithoutLosingElapsedTime()
    {
        Ticket ticket = open(2003L, "Wire", TicketPriority.CRITICAL, START);

        clock.advance(Duration.ofMinutes(30));
        sla.pause(ticket);
        clock.advance(Duration.ofMinutes(10));
        // Second pause while already paused must keep the original pause instant
        sla.pause(ticket);
        clock.advance(Duration.ofMinutes(20));
        sla.resume(ticket);

        // Extra resume when already running is a no-op
        sla.resume(ticket);
        assertThat(sla.evaluate(ticket).elapsed()).isEqualTo(Duration.ofMinutes(30));

        clock.advance(Duration.ofMinutes(40));
        sla.pause(ticket);
        clock.advance(Duration.ofHours(1));
        assertThat(sla.evaluate(ticket).elapsed()).isEqualTo(Duration.ofMinutes(70));
        assertThat(sla.evaluate(ticket).state()).isEqualTo(SlaState.PAUSED);

        sla.resume(ticket);
        clock.advance(Duration.ofMinutes(26));
        SlaSnapshot warning = sla.evaluate(ticket);
        assertThat(warning.elapsed()).isEqualTo(Duration.ofMinutes(96));
        assertThat(warning.percentUsed()).isEqualTo(80);
        assertThat(warning.state()).isEqualTo(SlaState.AT_RISK);
    }

    @Test
    void overdueTicketReportsPercentOverHundredAndNegativeRemaining()
    {
        Ticket ticket = open(2004L, "Locked account", TicketPriority.CRITICAL, START);
        clock.advance(Duration.ofMinutes(180));

        SlaSnapshot snapshot = sla.evaluate(ticket);

        assertThat(snapshot.state()).isEqualTo(SlaState.BREACHED);
        assertThat(snapshot.percentUsed()).isEqualTo(150);
        assertThat(snapshot.remaining()).isEqualTo(Duration.ofMinutes(-60));
    }

    @Test
    void resolvingTicketFreezesItsSlaClockAsMet()
    {
        Ticket ticket = open(2005L, "Resolved", TicketPriority.CRITICAL, START);
        clock.advance(Duration.ofMinutes(40));
        sla.pause(ticket);
        clock.advance(Duration.ofMinutes(40));
        sla.resolve(ticket);

        clock.advance(Duration.ofHours(5));
        SlaSnapshot snapshot = sla.evaluate(ticket);

        assertThat(ticket.isOpen()).isFalse();
        assertThat(snapshot.paused()).isFalse();
        assertThat(snapshot.elapsed()).isEqualTo(Duration.ofMinutes(40));
        assertThat(snapshot.percentUsed()).isEqualTo(33);
        assertThat(snapshot.state()).isEqualTo(SlaState.MET);
        assertThat(ticket.getSlaState()).isEqualTo(SlaState.MET);
    }

    @Test
    void ticketPropertiesRejectMissingOrSubSecondTargets()
    {
        assertThatThrownBy(() -> new TicketProperties.Sla(80, Map.of(
                TicketPriority.LOW, Duration.ofHours(72))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SLA target missing");

        assertThatThrownBy(() -> new TicketProperties.Sla(80, Map.of(
                TicketPriority.LOW, Duration.ofHours(72),
                TicketPriority.MEDIUM, Duration.ofHours(24),
                TicketPriority.HIGH, Duration.ofHours(8),
                TicketPriority.CRITICAL, Duration.ofMillis(500))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1 second");
    }

    private Ticket open(Long number, String title, TicketPriority priority, Instant startedAt)
    {
        return new Ticket(number, null, null, title, "description", priority,
                TicketOrigin.MANUAL, Platform.MOBILE, null, null, null,
                startedAt, sla.dueAt(priority, startedAt));
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
