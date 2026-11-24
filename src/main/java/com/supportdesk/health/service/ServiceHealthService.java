package com.supportdesk.health.service;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthNotice;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthResponse;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthRow;
import com.supportdesk.health.enums.BankService;
import com.supportdesk.health.enums.ServiceHealthStatus;
import com.supportdesk.health.service.ServiceHealthSignals.CustomerCount;
import com.supportdesk.health.service.ServiceHealthSignals.IncidentHit;
import com.supportdesk.health.service.ServiceHealthSignals.NoticeHit;
import com.supportdesk.monitor.entity.MonitorJobRun;
import com.supportdesk.monitor.repository.MonitorJobRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** The agent home panel. Computed on each request from incidents, error events and open auto-created tickets. */
@Service
@RequiredArgsConstructor
public class ServiceHealthService
{
    /** Several missed monitor runs (the job itself is every minute) before a quiet row becomes UNKNOWN. */
    static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private static final List<Platform> PLATFORMS = List.of(Platform.MOBILE, Platform.WEB);

    private final ServiceHealthSignals signals;
    private final ServiceHealthSettingsService settings;
    private final MonitorJobRunRepository monitorRuns;
    private final Clock clock;


    @Transactional(readOnly = true)
    public ServiceHealthResponse panel()
    {
        Instant now = clock.instant();
        var thresholds = settings.current();
        int windowMinutes = thresholds.windowMinutes();
        int minCustomers = thresholds.minCustomers();
        Instant from = now.minus(Duration.ofMinutes(windowMinutes));

        Optional<MonitorJobRun> latest = monitorRuns.findFirstByOrderByStartedAtDescIdDesc();
        Instant lastActivity = latest.map(ServiceHealthService::activityAt).orElse(null);
        boolean dataStale = lastActivity == null || lastActivity.isBefore(now.minus(STALE_AFTER));

        Map<Key, List<Long>> incidents = new LinkedHashMap<>();
        for (IncidentHit hit : signals.openIncidentsWithOpenTickets()) {
            String service = upper(hit.service());
            if (known(hit.platform(), service)) {
                incidents.computeIfAbsent(new Key(hit.platform(), service), k -> new ArrayList<>())
                        .add(hit.incidentId());
            }
        }

        Map<Key, Integer> customers = new LinkedHashMap<>();
        for (CustomerCount count : signals.distinctCustomersSince(from)) {
            String service = upper(count.service());
            if (known(count.platform(), service)) {
                customers.put(new Key(count.platform(), service), count.distinctCustomers());
            }
        }

        Map<Key, Map<Long, boolean[]>> notices = new LinkedHashMap<>();
        for (NoticeHit hit : signals.openAutoTicketNotices()) {
            String service = upper(hit.service());
            if (!known(hit.platform(), service)) {
                continue;
            }
            notices.computeIfAbsent(new Key(hit.platform(), service), k -> new LinkedHashMap<>())
                    .merge(hit.ticketNumber(), new boolean[] {hit.critical(), hit.slaBreached()},
                            (left, right) -> new boolean[] {left[0] || right[0], left[1] || right[1]});
        }

        List<ServiceHealthRow> rows = new ArrayList<>();
        for (BankService service : BankService.values()) {
            for (Platform platform : PLATFORMS) {
                Key key = new Key(platform, service.name());
                List<Long> incidentIds = incidents.getOrDefault(key, List.of());
                int distinct = customers.getOrDefault(key, 0);
                rows.add(new ServiceHealthRow(service.name(), platform,
                        ServiceHealthClassifier.classify(!incidentIds.isEmpty(), distinct, minCustomers, dataStale),
                        distinct, incidentIds, noticeList(notices.get(key))));
            }
        }
        rows.sort(ROW_ORDER);
        return new ServiceHealthResponse(dataStale, now, lastActivity, windowMinutes, minCustomers, rows);
    }

    private static List<ServiceHealthNotice> noticeList(Map<Long, boolean[]> byTicket)
    {
        if (byTicket == null || byTicket.isEmpty()) {
            return List.of();
        }
        return byTicket.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ServiceHealthNotice(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private static String upper(String service)
    {
        return service == null ? "" : service.toUpperCase(Locale.ROOT);
    }

    private static boolean known(Platform platform, String service)
    {
        return (platform == Platform.MOBILE || platform == Platform.WEB) && BankService.known(service);
    }

    private static Instant activityAt(MonitorJobRun run)
    {
        return run.getFinishedAt() != null ? run.getFinishedAt() : run.getStartedAt();
    }

    private static final Comparator<ServiceHealthRow> ROW_ORDER = Comparator
            .comparingInt((ServiceHealthRow row) -> switch (row.status()) {
                case DOWN -> 0;
                case DEGRADED -> 1;
                case UNKNOWN -> 2;
                case OPERATIONAL -> 3;
            })
            .thenComparing(ServiceHealthRow::service)
            .thenComparing(row -> row.platform().name());

    private record Key(Platform platform, String service) {}
}
