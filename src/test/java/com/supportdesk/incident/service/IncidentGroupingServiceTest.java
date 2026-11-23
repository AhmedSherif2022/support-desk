package com.supportdesk.incident.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.incident.entity.Incident;
import com.supportdesk.incident.entity.IncidentTicket;
import com.supportdesk.incident.enums.IncidentStatus;
import com.supportdesk.incident.repository.IncidentRepository;
import com.supportdesk.incident.repository.IncidentTicketRepository;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.repository.DetectedErrorEventRepository;
import com.supportdesk.ticket.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * E6-S3 grouping rules on their own: distinct customers (not rows), same error code, same platform, the time window,
 * the configured thresholds, and reuse of an open incident. The database side (unique links, reprocessing, concurrency)
 * is in IncidentGroupingIntegrationTest.
 */
class IncidentGroupingServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-22T10:30:00Z");

    private final DetectedErrorEventRepository eventRepository = mock(DetectedErrorEventRepository.class);
    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final IncidentTicketRepository incidentTicketRepository = mock(IncidentTicketRepository.class);
    private final IncidentSettingsService settings = mock(IncidentSettingsService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private IncidentGroupingService service;

    private final List<DetectedErrorEvent> candidates = new ArrayList<>();
    private long nextTicketId = 100;

    @BeforeEach
    void setUp()
    {
        service = new IncidentGroupingService(eventRepository, incidentRepository, incidentTicketRepository, settings,
                auditService, jdbc, Clock.fixed(NOW, ZoneOffset.UTC));
        when(settings.minAffectedCustomers()).thenReturn(5);
        when(settings.groupingWindow()).thenReturn(Duration.ofMinutes(15));
        when(eventRepository.findGroupingCandidates(anyCollection(), any(), any())).thenReturn(candidates);
        when(eventRepository.findEarliestOccurredHandledSince(any())).thenReturn(Optional.empty());
        when(incidentRepository.findByErrorCodeAndPlatformAndStatus(any(), any(), any())).thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));
        when(incidentTicketRepository.countDistinctCustomers(any())).thenReturn(5);
    }


    // ---- the threshold ----

    @Test
    void fiveDifferentCustomersSameCodeAndPlatformWithinTheWindowMakeOneIncidentAndEveryTicketCritical()
    {
        List<Ticket> tickets = List.of(
                event(1, "ERR_AUTH_503", Platform.MOBILE, 20),
                event(2, "ERR_AUTH_503", Platform.MOBILE, 12),
                event(3, "ERR_AUTH_503", Platform.MOBILE, 9),
                event(4, "ERR_AUTH_503", Platform.MOBILE, 7),
                event(5, "ERR_AUTH_503", Platform.MOBILE, 5));

        int linked = service.groupRecentFailures();

        assertThat(linked).isEqualTo(5);
        ArgumentCaptor<Incident> incident = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository, times(1)).save(incident.capture());
        assertThat(incident.getValue().getErrorCode()).isEqualTo("ERR_AUTH_503");
        assertThat(incident.getValue().getPlatform()).isEqualTo(Platform.MOBILE);
        assertThat(incident.getValue().getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident.getValue().getAffectedCustomerCount()).isEqualTo(5);
        assertThat(incident.getValue().getFirstOccurredAt()).isEqualTo(NOW.minusSeconds(20 * 60));
        assertThat(incident.getValue().getLastOccurredAt()).isEqualTo(NOW.minusSeconds(5 * 60));

        verify(incidentTicketRepository, times(5)).save(any(IncidentTicket.class));
        tickets.forEach(t -> verify(t).raisePriorityToCritical());
        assertThat(candidates).allMatch(e -> e.getOutcome() == EventOutcome.GROUPED);
        verify(jdbc).execute(contains("pg_advisory_xact_lock"));
    }

    @Test
    void incidentCreationIsAuditedWithItsNumbersAndNoCustomerData()
    {
        for (int c = 1; c <= 5; c++) {
            event(c, "PAY_TIMEOUT", Platform.WEB, 10 - c);
        }

        service.groupRecentFailures();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> after = ArgumentCaptor.forClass(Map.class);
        verify(auditService).record(eq(AuditAction.INCIDENT_CREATED), isNull(), eq(IncidentGroupingService.ENTITY),
                any(), isNull(), after.capture());
        assertThat(after.getValue())
                .containsEntry("errorCode", "PAY_TIMEOUT")
                .containsEntry("platform", "WEB")
                .containsEntry("affectedCustomerCount", 5)
                .containsKeys("incidentId", "service", "ticketNumbers")
                .doesNotContainKeys("nationalId", "mobile", "customerId");
        assertThat((List<?>) after.getValue().get("ticketNumbers")).hasSize(5);
    }

    @Test
    void fourDifferentCustomersMakeNoIncident()
    {
        List<Ticket> tickets = List.of(
                event(1, "ERR_AUTH_503", Platform.MOBILE, 10),
                event(2, "ERR_AUTH_503", Platform.MOBILE, 8),
                event(3, "ERR_AUTH_503", Platform.MOBILE, 6),
                event(4, "ERR_AUTH_503", Platform.MOBILE, 4));

        assertThat(service.groupRecentFailures()).isZero();

        assertNothingGrouped(tickets);
    }

    @Test
    void fiveErrorsFromOneCustomerAreOneCustomerNotFive()
    {
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tickets.add(event(1, "ERR_AUTH_503", Platform.MOBILE, 10 - i));
        }

        assertThat(service.groupRecentFailures()).isZero();

        assertNothingGrouped(tickets);
    }


    // ---- the grouping key ----

    @Test
    void differentErrorCodesAreSeparateGroups()
    {
        List<Ticket> authTickets = new ArrayList<>();
        List<Ticket> payTickets = new ArrayList<>();
        for (int c = 1; c <= 5; c++) {
            authTickets.add(event(c, "ERR_AUTH_503", Platform.MOBILE, 10 - c));
        }
        for (int c = 11; c <= 14; c++) {
            payTickets.add(event(c, "PAY_TIMEOUT", Platform.MOBILE, 20 - c));
        }

        assertThat(service.groupRecentFailures()).isEqualTo(5);

        ArgumentCaptor<Incident> incident = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository, times(1)).save(incident.capture());
        assertThat(incident.getValue().getErrorCode()).isEqualTo("ERR_AUTH_503");
        authTickets.forEach(t -> verify(t).raisePriorityToCritical());
        payTickets.forEach(t -> verify(t, never()).raisePriorityToCritical());
    }

    @Test
    void sameErrorCodeOnDifferentPlatformsIsSeparateGroups()
    {
        List<Ticket> mobile = new ArrayList<>();
        List<Ticket> web = new ArrayList<>();
        for (int c = 1; c <= 3; c++) {
            mobile.add(event(c, "ERR_AUTH_503", Platform.MOBILE, 10 - c));
        }
        for (int c = 11; c <= 13; c++) {
            web.add(event(c, "ERR_AUTH_503", Platform.WEB, 20 - c));
        }

        assertThat(service.groupRecentFailures()).isZero();        // 3 + 3 never adds up to 5

        verify(incidentRepository, never()).save(any());
        mobile.forEach(t -> verify(t, never()).raisePriorityToCritical());
        web.forEach(t -> verify(t, never()).raisePriorityToCritical());
    }


    // ---- the window ----

    @Test
    void anErrorOutsideTheWindowDoesNotCountTowardsTheFive()
    {
        List<Ticket> tickets = List.of(
                event(1, "ERR_AUTH_503", Platform.MOBILE, 40),        // 30 minutes before the others
                event(2, "ERR_AUTH_503", Platform.MOBILE, 10),
                event(3, "ERR_AUTH_503", Platform.MOBILE, 8),
                event(4, "ERR_AUTH_503", Platform.MOBILE, 6),
                event(5, "ERR_AUTH_503", Platform.MOBILE, 4));

        assertThat(service.groupRecentFailures()).isZero();

        assertNothingGrouped(tickets);
    }

    @Test
    void fiveCustomersSpreadOverExactlyTheWindowStillGroup()
    {
        for (int c = 1; c <= 5; c++) {
            event(c, "ERR_AUTH_503", Platform.MOBILE, 20 - (c - 1) * 15 / 4);     // 20, 17, 13, 10, 5 minutes ago
        }

        assertThat(service.groupRecentFailures()).isEqualTo(5);
    }

    @Test
    void anOldLoneErrorStaysOutOfTheIncidentTheOthersForm()
    {
        Ticket old = event(9, "ERR_AUTH_503", Platform.MOBILE, 60);
        for (int c = 1; c <= 5; c++) {
            event(c, "ERR_AUTH_503", Platform.MOBILE, 10 - c);
        }

        assertThat(service.groupRecentFailures()).isEqualTo(5);

        verify(old, never()).raisePriorityToCritical();
        assertThat(candidates.get(0).getOutcome()).isEqualTo(EventOutcome.CREATED);
    }


    // ---- the thresholds come from the settings ----

    @Test
    void thresholdsAreReadFromTheSettingsNotHardcoded()
    {
        when(settings.minAffectedCustomers()).thenReturn(3);
        when(settings.groupingWindow()).thenReturn(Duration.ofMinutes(5));
        event(1, "ERR_AUTH_503", Platform.MOBILE, 4);
        event(2, "ERR_AUTH_503", Platform.MOBILE, 3);
        event(3, "ERR_AUTH_503", Platform.MOBILE, 2);
        Ticket late = event(4, "ERR_AUTH_503", Platform.MOBILE, 30);   // 26 minutes before: outside a 5-minute window

        assertThat(service.groupRecentFailures()).isEqualTo(3);

        verify(settings).minAffectedCustomers();
        verify(settings).groupingWindow();
        verify(late, never()).raisePriorityToCritical();
    }


    // ---- an open incident is reused ----

    @Test
    void newErrorsWithinTheWindowOfAnOpenIncidentJoinItInsteadOfOpeningAnother()
    {
        Incident open = new Incident("ERR_AUTH_503", Platform.MOBILE, "AUTH", NOW.minusSeconds(30 * 60), NOW.minusSeconds(20 * 60));
        when(incidentRepository.findByErrorCodeAndPlatformAndStatus("ERR_AUTH_503", Platform.MOBILE, IncidentStatus.OPEN))
                .thenReturn(Optional.of(open));
        when(incidentTicketRepository.countDistinctCustomers(any())).thenReturn(7);
        Ticket a = event(6, "ERR_AUTH_503", Platform.MOBILE, 10);      // 10 min after the incident's last error
        Ticket b = event(7, "ERR_AUTH_503", Platform.MOBILE, 2);       // chained: 8 min after a

        assertThat(service.groupRecentFailures()).isEqualTo(2);

        verify(incidentRepository, never()).save(any());
        verify(auditService, never()).record(eq(AuditAction.INCIDENT_CREATED), any(), any(), any(), any(), any());
        verify(auditService, times(1)).record(eq(AuditAction.INCIDENT_TICKETS_LINKED), isNull(),
                eq(IncidentGroupingService.ENTITY), any(), isNull(), any());
        verify(a).raisePriorityToCritical();
        verify(b).raisePriorityToCritical();
        ArgumentCaptor<IncidentTicket> links = ArgumentCaptor.forClass(IncidentTicket.class);
        verify(incidentTicketRepository, times(2)).save(links.capture());
        assertThat(links.getAllValues()).allMatch(l -> l.getIncident() == open);
        assertThat(open.getLastOccurredAt()).isEqualTo(NOW.minusSeconds(2 * 60));
        assertThat(open.getAffectedCustomerCount()).isEqualTo(7);
    }

    @Test
    void anErrorLongAfterAnOpenIncidentsLastErrorDoesNotJoinIt()
    {
        Incident open = new Incident("ERR_AUTH_503", Platform.MOBILE, "AUTH", NOW.minusSeconds(90 * 60), NOW.minusSeconds(60 * 60));
        when(incidentRepository.findByErrorCodeAndPlatformAndStatus("ERR_AUTH_503", Platform.MOBILE, IncidentStatus.OPEN))
                .thenReturn(Optional.of(open));
        Ticket late = event(6, "ERR_AUTH_503", Platform.MOBILE, 5);    // 55 minutes after the incident's last error

        assertThat(service.groupRecentFailures()).isZero();

        verify(late, never()).raisePriorityToCritical();
        verify(incidentTicketRepository, never()).save(any());
        verify(incidentRepository, never()).save(any());                // a second OPEN incident is never opened
    }

    @Test
    void anOlderErrorInTheLookBackDoesNotPullTheAnchorBackAndDropANewerOne()
    {
        // The review case: 15-minute window, incident's last error 20 minutes ago.
        Incident open = new Incident("ERR_AUTH_503", Platform.MOBILE, "AUTH", NOW.minusSeconds(40 * 60), NOW.minusSeconds(20 * 60));
        when(incidentRepository.findByErrorCodeAndPlatformAndStatus("ERR_AUTH_503", Platform.MOBILE, IncidentStatus.OPEN))
                .thenReturn(Optional.of(open));
        Ticket older = event(6, "ERR_AUTH_503", Platform.MOBILE, 30);  // in the look-back: 10 min before the last error
        Ticket newer = event(7, "ERR_AUTH_503", Platform.MOBILE, 8);   // 12 min after the last error: inside the window

        assertThat(service.groupRecentFailures()).isEqualTo(2);

        verify(older).raisePriorityToCritical();
        verify(newer).raisePriorityToCritical();
        assertThat(open.getLastOccurredAt()).isEqualTo(NOW.minusSeconds(8 * 60));
        assertThat(open.getFirstOccurredAt()).isEqualTo(NOW.minusSeconds(40 * 60));
    }

    @Test
    void aLaterBurstOfFiveCustomersJoinsTheOpenIncidentInsteadOfStayingSeparate()
    {
        Incident open = new Incident("ERR_AUTH_503", Platform.MOBILE, "AUTH", NOW.minusSeconds(90 * 60), NOW.minusSeconds(60 * 60));
        when(incidentRepository.findByErrorCodeAndPlatformAndStatus("ERR_AUTH_503", Platform.MOBILE, IncidentStatus.OPEN))
                .thenReturn(Optional.of(open));
        when(incidentTicketRepository.countDistinctCustomers(any())).thenReturn(10);
        List<Ticket> wave = new ArrayList<>();
        for (int c = 11; c <= 15; c++) {
            wave.add(event(c, "ERR_AUTH_503", Platform.MOBILE, 16 - c));    // 5 .. 1 min ago: 55 min after the last error
        }

        assertThat(service.groupRecentFailures()).isEqualTo(5);

        verify(incidentRepository, never()).save(any());                // still one OPEN incident per code and platform
        verify(auditService, never()).record(eq(AuditAction.INCIDENT_CREATED), any(), any(), any(), any(), any());
        verify(auditService, times(1)).record(eq(AuditAction.INCIDENT_TICKETS_LINKED), any(), any(), any(), any(), any());
        wave.forEach(t -> verify(t).raisePriorityToCritical());
        ArgumentCaptor<IncidentTicket> links = ArgumentCaptor.forClass(IncidentTicket.class);
        verify(incidentTicketRepository, times(5)).save(links.capture());
        assertThat(links.getAllValues()).allMatch(l -> l.getIncident() == open);
        assertThat(open.getLastOccurredAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(open.getAffectedCustomerCount()).isEqualTo(10);
        assertThat(candidates).allMatch(e -> e.getOutcome() == EventOutcome.GROUPED);
    }

    @Test
    void twoBurstsInOneRunMakeOneIncidentNotTwo()
    {
        for (int c = 1; c <= 5; c++) {
            event(c, "ERR_AUTH_503", Platform.MOBILE, 30 - c);              // 29 .. 25 min ago
        }
        for (int c = 11; c <= 15; c++) {
            event(c, "ERR_AUTH_503", Platform.MOBILE, 16 - c);              // 5 .. 1 min ago: 20 min later
        }

        assertThat(service.groupRecentFailures()).isEqualTo(10);

        verify(incidentRepository, times(1)).save(any(Incident.class));
        verify(auditService, times(1)).record(eq(AuditAction.INCIDENT_CREATED), any(), any(), any(), any(), any());
        verify(auditService, times(1)).record(eq(AuditAction.INCIDENT_TICKETS_LINKED), any(), any(), any(), any(), any());
        verify(incidentTicketRepository, times(10)).save(any(IncidentTicket.class));
    }

    @Test
    void anErrorJustBeforeTheBusiestWindowJoinsTheNewIncident()
    {
        Ticket early = event(9, "ERR_AUTH_503", Platform.MOBILE, 30);  // 12 min before the burst: within one window
        for (int c = 1; c <= 6; c++) {
            event(c, "ERR_AUTH_503", Platform.MOBILE, 19 - c);              // 18 .. 13 min ago: the busiest window
        }

        assertThat(service.groupRecentFailures()).isEqualTo(7);

        verify(early).raisePriorityToCritical();
        ArgumentCaptor<Incident> incident = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository, times(1)).save(incident.capture());
        assertThat(incident.getValue().getFirstOccurredAt()).isEqualTo(NOW.minusSeconds(30 * 60));
        assertThat(candidates).allMatch(e -> e.getOutcome() == EventOutcome.GROUPED);
    }

    @Test
    void onlyErrorsHandledInTheLastTwoWindowsAreRead()
    {
        service.groupRecentFailures();

        Instant since = NOW.minusSeconds(30 * 60);
        verify(eventRepository).findGroupingCandidates(anyCollection(), eq(since), eq(since));   // nothing handled late
    }

    @Test
    void aRowHandledLateAlsoReadsTheRowsThatOccurredNearIt()
    {
        // The earliest row handled in the last two windows occurred an hour ago: read from one window before that.
        when(eventRepository.findEarliestOccurredHandledSince(any())).thenReturn(Optional.of(NOW.minusSeconds(60 * 60)));

        service.groupRecentFailures();

        verify(eventRepository).findGroupingCandidates(anyCollection(), eq(NOW.minusSeconds(30 * 60)),
                eq(NOW.minusSeconds(75 * 60)));
    }

    @Test
    void nothingToGroupDoesNothing()
    {
        assertThat(service.groupRecentFailures()).isZero();

        verify(incidentRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }


    // ---- helpers ----

    /** One handled error of {@code customerId}, {@code minutesAgo}, on its own automatic ticket (returned). */
    private Ticket event(long customerId, String code, Platform platform, int minutesAgo)
    {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);
        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn(nextTicketId++);
        when(ticket.getTicketNumber()).thenReturn(2000 + nextTicketId);
        DetectedErrorEvent event = new DetectedErrorEvent(customer, code, platform, "AUTH", "msg", "corr-" + nextTicketId,
                NOW.minusSeconds(minutesAgo * 60L));
        event.markProcessed(EventOutcome.CREATED, ticket, NOW);
        candidates.add(event);
        candidates.sort((x, y) -> x.getErrorCode().equals(y.getErrorCode())
                ? (x.getPlatform() == y.getPlatform() ? x.getOccurredAt().compareTo(y.getOccurredAt())
                        : x.getPlatform().compareTo(y.getPlatform()))
                : x.getErrorCode().compareTo(y.getErrorCode()));
        return ticket;
    }

    private void assertNothingGrouped(List<Ticket> tickets)
    {
        verify(incidentRepository, never()).save(any());
        verify(incidentTicketRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
        tickets.forEach(t -> verify(t, never()).raisePriorityToCritical());
        assertThat(candidates).allMatch(e -> e.getOutcome() == EventOutcome.CREATED);
    }
}
