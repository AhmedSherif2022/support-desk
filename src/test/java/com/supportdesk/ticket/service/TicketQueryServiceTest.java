package com.supportdesk.ticket.service;

import com.supportdesk.auth.entity.User;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.guide.service.GuideMatcher;
import com.supportdesk.ticket.enums.QueueChip;
import com.supportdesk.ticket.enums.QueueSort;
import com.supportdesk.ticket.config.TicketProperties;
import com.supportdesk.ticket.dto.QueueResponse;
import com.supportdesk.ticket.dto.QueueTicketResponse;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketQueryServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    @Mock
    private TicketRepository tickets;

    private SlaService slaService;
    private TicketQueryService query;
    private Customer sara;
    private User mona;
    private TicketCategory paymentFailed;

    @BeforeEach
    void setUp()
    {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        slaService = new SlaService(properties(), clock);
        GuideMatcher guides = mock(GuideMatcher.class);
        lenient().when(guides.loadAvailability()).thenReturn(ticket -> true);
        query = new TicketQueryService(tickets, slaService, guides);

        sara = mock(Customer.class);
        lenient().when(sara.getCif()).thenReturn("CIF-100245");
        lenient().when(sara.getFullName()).thenReturn("Sara Mahmoud");

        mona = mock(User.class);
        lenient().when(mona.getFullName()).thenReturn("Mona");

        TicketCategory payments = new TicketCategory("PAYMENTS", "Payments", null,
                TicketPriority.HIGH, false, null, null, null);
        paymentFailed = new TicketCategory("PAYMENT_FAILED", "Payment Failed", payments,
                TicketPriority.HIGH, true, "^TRX-\\d{4}-\\d{5}$", null, null);

        Ticket pausedTicket = open(4L, 2004L, TicketPriority.MEDIUM, NOW.minus(Duration.ofHours(10)));
        pausedTicket.moveTo(TicketStatus.WAITING_ON_CUSTOMER, NOW.minus(Duration.ofHours(8)));

        // lenient: the comparator test sorts rows directly and never reads the repository
        lenient().when(tickets.findByStatusNotIn(anyCollection())).thenReturn(List.of(
                open(1L, 2001L, TicketPriority.LOW, NOW.minus(Duration.ofHours(2))),
                open(2L, 2002L, TicketPriority.HIGH, NOW.minus(Duration.ofHours(6).plusMinutes(24))),
                open(3L, 2003L, TicketPriority.CRITICAL, NOW.minus(Duration.ofMinutes(180))),
                pausedTicket));
    }

    @Test
    void wholeQueueIsRiskiestFirstWithPausedLastAndCountsEveryChip()
    {
        QueueResponse queue = query.queue(null, null);

        assertThat(queue.chip()).isEqualTo(QueueChip.ALL);
        assertThat(queue.tickets()).extracting(QueueTicketResponse::number)
                .containsExactly("#2003", "#2002", "#2001", "#2004");   // E3-S4: paused tickets at the bottom
        assertThat(queue.counts())
                .containsEntry("ALL", 4L)
                .containsEntry("AT_RISK", 2L)          // at risk or breached
                .containsEntry("IN_REVIEW", 0L)
                .containsEntry("AUTO_CREATED", 0L)
                .hasSize(4);
    }

    @Test
    void atRiskChipShowsAtRiskAndBreachedAndKeepsTheOtherCounts()
    {
        QueueResponse queue = query.queue(QueueChip.AT_RISK, null);

        assertThat(queue.tickets()).extracting(QueueTicketResponse::number, QueueTicketResponse::slaState)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("#2003", SlaState.BREACHED),
                        org.assertj.core.groups.Tuple.tuple("#2002", SlaState.AT_RISK));
        assertThat(queue.counts()).containsEntry("ALL", 4L).containsEntry("AT_RISK", 2L);
    }

    @Test
    void inReviewAndAutoCreatedChipsFilter()
    {
        Ticket confirm = open(5L, 2005L, TicketPriority.LOW, NOW.minus(Duration.ofHours(1)));
        confirm.moveTo(TicketStatus.IN_REVIEW, NOW);
        Ticket auto = new Ticket(2006L, sara, paymentFailed, "Auto", "description", TicketPriority.MEDIUM,
                TicketOrigin.AUTO_CREATED, Platform.WEB, "ERR_AUTH_503", "TRX-2026-01111", mona,
                NOW.minus(Duration.ofHours(1)), slaService.dueAt(TicketPriority.MEDIUM, NOW.minus(Duration.ofHours(1))));
        ReflectionTestUtils.setField(auto, "id", 6L);
        when(tickets.findByStatusNotIn(anyCollection())).thenReturn(List.of(
                open(3L, 2003L, TicketPriority.CRITICAL, NOW.minus(Duration.ofMinutes(180))), confirm, auto));

        QueueResponse all = query.queue(QueueChip.ALL, null);
        assertThat(all.counts()).containsEntry("IN_REVIEW", 1L).containsEntry("AUTO_CREATED", 1L);

        assertThat(query.queue(QueueChip.IN_REVIEW, null).tickets()).singleElement()
                .extracting(QueueTicketResponse::number, QueueTicketResponse::nextActionLabel)
                .containsExactly("#2005", "Confirm fix");
        assertThat(query.queue(QueueChip.AUTO_CREATED, null).tickets()).singleElement()
                .extracting(QueueTicketResponse::number, QueueTicketResponse::origin, QueueTicketResponse::errorCode)
                .containsExactly("#2006", TicketOrigin.AUTO_CREATED, "ERR_AUTH_503");
    }

    @Test
    void tiesBreakByLeastRemainingTimeAndPausedStaysLast()
    {
        // HIGH (6h24m elapsed -> 80%, 96m left) and CRITICAL (96m elapsed -> 80%, 24m left) both sit at 80%
        Ticket highWarning = open(10L, 2010L, TicketPriority.HIGH, NOW.minus(Duration.ofHours(6).plusMinutes(24)));
        Ticket criticalPaused = open(11L, 2011L, TicketPriority.CRITICAL, NOW.minus(Duration.ofMinutes(106)));
        criticalPaused.moveTo(TicketStatus.WAITING_ON_CUSTOMER, NOW.minus(Duration.ofMinutes(10)));
        Ticket criticalRunning = open(12L, 2012L, TicketPriority.CRITICAL, NOW.minus(Duration.ofMinutes(96)));

        when(tickets.findByStatusNotIn(anyCollection()))
                .thenReturn(List.of(highWarning, criticalPaused, criticalRunning));

        QueueResponse queue = query.queue(QueueChip.ALL, null);

        assertThat(queue.tickets()).extracting(QueueTicketResponse::number)
                .containsExactly("#2012", "#2010", "#2011");
        assertThat(queue.tickets().get(2).nextActionLabel()).isEqualTo("Resume");
    }

    @Test
    void slaSortIsTheDefaultAndMatchesTheRiskOrder()
    {
        List<String> byDefault = query.queue(null, null).tickets().stream().map(QueueTicketResponse::number).toList();
        List<String> bySla = query.queue(null, QueueSort.SLA).tickets().stream().map(QueueTicketResponse::number).toList();

        assertThat(byDefault).containsExactly("#2003", "#2002", "#2001", "#2004");
        assertThat(bySla).isEqualTo(byDefault);
    }

    @Test
    void prioritySortPutsCriticalFirstThenHighMediumLowAndPausedLast()
    {
        // SLA order would be LOW (breached), MEDIUM (83%), HIGH (37%), HIGH (12%), CRITICAL (8%), paused CRITICAL
        Ticket lowBreached = open(20L, 2020L, TicketPriority.LOW, NOW.minus(Duration.ofHours(80)));
        Ticket mediumWarning = open(21L, 2021L, TicketPriority.MEDIUM, NOW.minus(Duration.ofHours(20)));
        Ticket highOlder = open(22L, 2022L, TicketPriority.HIGH, NOW.minus(Duration.ofHours(3)));
        Ticket highNewer = open(23L, 2023L, TicketPriority.HIGH, NOW.minus(Duration.ofHours(1)));
        Ticket criticalFresh = open(24L, 2024L, TicketPriority.CRITICAL, NOW.minus(Duration.ofMinutes(10)));
        Ticket criticalPaused = open(25L, 2025L, TicketPriority.CRITICAL, NOW.minus(Duration.ofMinutes(100)));
        criticalPaused.moveTo(TicketStatus.WAITING_ON_CUSTOMER, NOW.minus(Duration.ofMinutes(5)));

        when(tickets.findByStatusNotIn(anyCollection())).thenReturn(List.of(
                lowBreached, highNewer, criticalPaused, mediumWarning, criticalFresh, highOlder));

        QueueResponse bySla = query.queue(QueueChip.ALL, QueueSort.SLA);
        assertThat(bySla.tickets()).extracting(QueueTicketResponse::number)
                .containsExactly("#2020", "#2021", "#2022", "#2023", "#2024", "#2025");

        QueueResponse byPriority = query.queue(QueueChip.ALL, QueueSort.PRIORITY);
        assertThat(byPriority.tickets()).extracting(QueueTicketResponse::number)
                .containsExactly("#2024", "#2022", "#2023", "#2021", "#2020", "#2025");   // ties inside HIGH in SLA order
        assertThat(byPriority.tickets()).extracting(QueueTicketResponse::priority)
                .containsExactly(TicketPriority.CRITICAL, TicketPriority.HIGH, TicketPriority.HIGH,
                        TicketPriority.MEDIUM, TicketPriority.LOW, TicketPriority.CRITICAL);
        assertThat(byPriority.tickets().get(5).paused()).isTrue();
        assertThat(byPriority.counts()).isEqualTo(bySla.counts());
    }

    @Test
    void prioritySortIsAppliedBeforeTheChipFilter()
    {
        Ticket lowBreached = open(30L, 2030L, TicketPriority.LOW, NOW.minus(Duration.ofHours(80)));
        Ticket highAtRisk = open(31L, 2031L, TicketPriority.HIGH, NOW.minus(Duration.ofHours(7)));
        Ticket criticalBreached = open(32L, 2032L, TicketPriority.CRITICAL, NOW.minus(Duration.ofHours(3)));
        Ticket mediumFresh = open(33L, 2033L, TicketPriority.MEDIUM, NOW.minus(Duration.ofHours(1)));

        when(tickets.findByStatusNotIn(anyCollection())).thenReturn(List.of(
                lowBreached, highAtRisk, criticalBreached, mediumFresh));

        QueueResponse atRisk = query.queue(QueueChip.AT_RISK, QueueSort.PRIORITY);

        assertThat(atRisk.tickets()).extracting(QueueTicketResponse::number)
                .containsExactly("#2032", "#2031", "#2030");
        assertThat(atRisk.counts()).containsEntry("ALL", 4L).containsEntry("AT_RISK", 3L);
    }

    @Test
    void comparatorsOrderRowsDirectly()
    {
        QueueTicketResponse lowBreached = row(1L, TicketPriority.LOW, 120, -600L, false);
        QueueTicketResponse mediumHalf = row(2L, TicketPriority.MEDIUM, 50, 3600L, false);
        QueueTicketResponse highLate = row(3L, TicketPriority.HIGH, 70, 1800L, false);
        QueueTicketResponse highEarly = row(4L, TicketPriority.HIGH, 70, 900L, false);
        QueueTicketResponse criticalFresh = row(5L, TicketPriority.CRITICAL, 5, 7000L, false);
        QueueTicketResponse criticalPaused = row(6L, TicketPriority.CRITICAL, 90, 600L, true);
        List<QueueTicketResponse> rows = List.of(mediumHalf, criticalPaused, highLate, criticalFresh, lowBreached, highEarly);

        assertThat(rows.stream().sorted(TicketQueryService.order(QueueSort.SLA)).map(QueueTicketResponse::id))
                .containsExactly(1L, 4L, 3L, 2L, 5L, 6L);
        assertThat(rows.stream().sorted(TicketQueryService.riskOrder()).map(QueueTicketResponse::id))
                .containsExactly(1L, 4L, 3L, 2L, 5L, 6L);
        assertThat(rows.stream().sorted(TicketQueryService.order(QueueSort.PRIORITY)).map(QueueTicketResponse::id))
                .containsExactly(5L, 4L, 3L, 2L, 1L, 6L);

        // and the service applies exactly these comparators to the loaded tickets
        assertThat(query.queue(null, QueueSort.PRIORITY).tickets()).extracting(QueueTicketResponse::number)
                .containsExactly("#2003", "#2002", "#2001", "#2004");
    }

    private static QueueTicketResponse row(Long id, TicketPriority priority, int percentUsed, long remainingSeconds,
            boolean paused)
    {
        return new QueueTicketResponse(id, "#" + (2000 + id), "Ticket", "CIF-100245", "Sara Mahmoud", null,
                "Payments / Payment Failed", Platform.MOBILE, null, TicketOrigin.MANUAL, priority,
                paused ? TicketStatus.WAITING_ON_CUSTOMER : TicketStatus.TROUBLESHOOTING, "Mona",
                percentUsed >= 100 ? SlaState.BREACHED : SlaState.ON_TRACK, "label", NOW, percentUsed,
                remainingSeconds, paused, null, null);
    }

    private Ticket open(Long id, Long ticketNumber, TicketPriority priority, Instant startedAt)
    {
        Ticket ticket = new Ticket(ticketNumber, sara, paymentFailed, "Ticket " + ticketNumber, "description",
                priority, TicketOrigin.MANUAL, Platform.MOBILE, null, "TRX-2026-01111", mona,
                startedAt, slaService.dueAt(priority, startedAt));
        ticket.assignTo(mona);
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
}
