package com.supportdesk.incident.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.incident.entity.Incident;
import com.supportdesk.incident.entity.IncidentTicket;
import com.supportdesk.incident.enums.IncidentStatus;
import com.supportdesk.incident.repository.IncidentRepository;
import com.supportdesk.incident.repository.IncidentTicketRepository;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.monitor.repository.DetectedErrorEventRepository;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * E6-S3: groups mass failures into one incident so SRE sees one problem.
 * <p>
 * One pass per monitor run: take every handled error whose automatic ticket is still open and not in an incident,
 * split them by error code + platform, and for each group either
 * <ul>
 *   <li>join the group's OPEN incident, if there is one, with the errors that fall within the grouping window of its
 *       latest error (each joined error stretches that window further), or</li>
 *   <li>open a new incident when some window of {@code groupingWindow} holds at least {@code minAffectedCustomers}
 *       <em>different</em> customers; the errors in that window, and those chained within a window after it, join it.</li>
 * </ul>
 * Every linked ticket becomes CRITICAL and its errors are marked GROUPED; a new incident is audited as INCIDENT_CREATED.
 * <p>
 * Concurrency: the whole pass holds a PostgreSQL transaction-level advisory lock, so two monitor runs (on any number
 * of instances) never group at the same time. Underneath, the partial unique index on open incidents and the unique
 * ticket_id on incident_ticket make a duplicate impossible even if the lock were ever bypassed.
 * <p>
 * E5 is postponed: nothing here escalates a ticket or creates an Azure work item. Thresholds come from
 * {@link IncidentSettingsService}, never from code.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentGroupingService
{
    public static final String ENTITY = "INCIDENT";

    /** Arbitrary but fixed: every instance asks for the same advisory lock. */
    static final long GROUPING_LOCK_KEY = 6_003L;

    private static final List<TicketStatus> CLOSED = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final DetectedErrorEventRepository eventRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentTicketRepository incidentTicketRepository;
    private final IncidentSettingsService settings;
    private final AuditService auditService;
    private final JdbcTemplate jdbc;
    private final Clock clock;


    /** @return how many tickets were linked to an incident in this pass */
    @Transactional
    public int groupRecentFailures()
    {
        // Shares the JPA transaction's connection; released automatically when the transaction ends.
        jdbc.execute("SELECT pg_advisory_xact_lock(" + GROUPING_LOCK_KEY + ")");

        int minCustomers = settings.minAffectedCustomers();
        Duration window = settings.groupingWindow();
        Instant now = clock.instant();

        Map<GroupKey, List<DetectedErrorEvent>> groups = eventRepository.findGroupingCandidates(CLOSED).stream()
                .collect(Collectors.groupingBy(e -> new GroupKey(e.getErrorCode(), e.getPlatform()),
                        LinkedHashMap::new, Collectors.toList()));

        int linked = 0;
        for (Map.Entry<GroupKey, List<DetectedErrorEvent>> group : groups.entrySet()) {
            linked += groupOne(group.getKey(), group.getValue(), minCustomers, window, now);
        }
        return linked;
    }


    private int groupOne(GroupKey key, List<DetectedErrorEvent> events, int minCustomers, Duration window, Instant now)
    {
        Optional<Incident> open = incidentRepository.findByErrorCodeAndPlatformAndStatus(
                key.errorCode(), key.platform(), IncidentStatus.OPEN);
        if (open.isPresent()) {
            Instant lastError = open.get().getLastOccurredAt();
            List<DetectedErrorEvent> joining = chain(events, lastError.minus(window), lastError, window);
            return joining.isEmpty() ? 0 : link(open.get(), joining, now, false);
        }

        Optional<Instant> start = densestWindowStart(events, window, minCustomers);
        if (start.isEmpty()) {
            return 0;                                                  // fewer customers than the threshold: tickets stay separate
        }
        List<DetectedErrorEvent> members = chain(events, start.get(), null, window);
        DetectedErrorEvent first = members.get(0);
        Incident incident = incidentRepository.save(new Incident(key.errorCode(), key.platform(), first.getService(),
                first.getOccurredAt(), members.get(members.size() - 1).getOccurredAt()));
        return link(incident, members, now, true);
    }


    /**
     * The start of the first window of {@code window} that holds at least {@code minCustomers} different customers,
     * preferring the window with the most customers. Empty when no window reaches the threshold.
     */
    private static Optional<Instant> densestWindowStart(List<DetectedErrorEvent> sorted, Duration window, int minCustomers)
    {
        Instant bestStart = null;
        int best = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Instant start = sorted.get(i).getOccurredAt();
            Instant end = start.plus(window);
            Set<Long> customers = new HashSet<>();
            for (int j = i; j < sorted.size() && !sorted.get(j).getOccurredAt().isAfter(end); j++) {
                customers.add(sorted.get(j).getCustomer().getId());
            }
            if (customers.size() > best) {
                best = customers.size();
                bestStart = start;
            }
        }
        return best >= minCustomers ? Optional.of(bestStart) : Optional.empty();
    }


    /**
     * Errors at or after {@code from}, walked in time order, while each is within {@code window} of the previous one
     * (or of {@code anchor}, the incident's latest error, for the first). A gap longer than the window ends the chain.
     */
    private static List<DetectedErrorEvent> chain(List<DetectedErrorEvent> sorted, Instant from, Instant anchor, Duration window)
    {
        List<DetectedErrorEvent> out = new ArrayList<>();
        Instant last = anchor;
        for (DetectedErrorEvent event : sorted) {
            Instant at = event.getOccurredAt();
            if (at.isBefore(from)) {
                continue;
            }
            if (last != null && at.isAfter(last.plus(window))) {
                break;
            }
            out.add(event);
            last = at;
        }
        return out;
    }


    /** Links the events' tickets (once each), makes them CRITICAL, marks the errors GROUPED and refreshes the counts. */
    private int link(Incident incident, List<DetectedErrorEvent> events, Instant now, boolean created)
    {
        Map<Long, Ticket> tickets = new LinkedHashMap<>();
        for (DetectedErrorEvent event : events) {
            tickets.putIfAbsent(event.getTicket().getId(), event.getTicket());
            event.markGrouped();
        }
        for (Ticket ticket : tickets.values()) {
            incidentTicketRepository.save(new IncidentTicket(incident, ticket, now));
            ticket.raisePriorityToCritical();
        }

        int affected = incidentTicketRepository.countDistinctCustomers(incident.getId());
        incident.recordLinkedErrors(events.get(0).getOccurredAt(), events.get(events.size() - 1).getOccurredAt(), affected);

        if (created) {
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("incidentId", incident.getId());
            after.put("errorCode", incident.getErrorCode());
            after.put("platform", incident.getPlatform().name());
            after.put("service", incident.getService());
            after.put("affectedCustomerCount", affected);
            after.put("ticketNumbers", tickets.values().stream().map(Ticket::getTicketNumber).toList());
            // No logged-in user in the monitor job: a null user records it as a system action.
            auditService.record(AuditAction.INCIDENT_CREATED, null, ENTITY, String.valueOf(incident.getId()), null, after);
            log.info("Incident {} opened for {} on {}: {} customers, {} tickets", incident.getId(),
                    incident.getErrorCode(), incident.getPlatform(), affected, tickets.size());
        }
        return tickets.size();
    }


    private record GroupKey(String errorCode, Platform platform)
    {
    }
}
