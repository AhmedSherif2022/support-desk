package com.supportdesk.health.service;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthNotice;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthResponse;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthRow;
import com.supportdesk.health.dto.ServiceHealthDtos.ServiceHealthSettingsDto;
import com.supportdesk.health.enums.ServiceHealthStatus;
import com.supportdesk.health.service.ServiceHealthSignals.CustomerCount;
import com.supportdesk.health.service.ServiceHealthSignals.IncidentHit;
import com.supportdesk.health.service.ServiceHealthSignals.NoticeHit;
import com.supportdesk.monitor.entity.MonitorJobRun;
import com.supportdesk.monitor.enums.JobTrigger;
import com.supportdesk.monitor.repository.MonitorJobRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceHealthServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

    private final FakeSignals signals = new FakeSignals();
    private final ServiceHealthSettingsService settings = mock(ServiceHealthSettingsService.class);
    private final MonitorJobRunRepository monitorRuns = mock(MonitorJobRunRepository.class);
    private ServiceHealthService service;

    @BeforeEach
    void setUp()
    {
        service = new ServiceHealthService(signals, settings, monitorRuns, Clock.fixed(NOW, ZoneOffset.UTC));
        when(settings.current()).thenReturn(new ServiceHealthSettingsDto(5, 15));
        finishedAt(NOW.minusSeconds(30));
    }

    @Test
    void everyFixedServiceAppearsOnMobileAndWebEvenWithNoErrors()
    {
        ServiceHealthResponse panel = service.panel();

        assertThat(panel.dataStale()).isFalse();
        assertThat(panel.services()).hasSize(8);
        assertThat(panel.services()).allMatch(row -> row.status() == ServiceHealthStatus.OPERATIONAL);
        assertThat(panel.services()).extracting(ServiceHealthRow::service)
                .contains("AUTH", "PAYMENTS", "CARDS", "TRANSFERS");
    }

    @Test
    void anOpenIncidentIsDownUntilItsTicketsAreClosedAndACriticalTicketIsOnlyANotice()
    {
        signals.incidents.add(new IncidentHit(Platform.MOBILE, "PAYMENTS", 12L));
        signals.notices.add(new NoticeHit(Platform.WEB, "auth", 2104L, true, true));

        ServiceHealthResponse panel = service.panel();

        ServiceHealthRow payments = row(panel, "PAYMENTS", Platform.MOBILE);
        assertThat(payments.status()).isEqualTo(ServiceHealthStatus.DOWN);
        assertThat(payments.openIncidentIds()).containsExactly(12L);

        ServiceHealthRow auth = row(panel, "AUTH", Platform.WEB);
        assertThat(auth.status()).isEqualTo(ServiceHealthStatus.OPERATIONAL);
        assertThat(auth.notices()).containsExactly(new ServiceHealthNotice(2104L, true, true));
    }

    @Test
    void fiveDistinctCustomersDegradeThatServiceOnly()
    {
        signals.counts.add(new CustomerCount(Platform.MOBILE, "TRANSFERS", 5));
        signals.counts.add(new CustomerCount(Platform.MOBILE, "CARDS", 4));

        ServiceHealthResponse panel = service.panel();

        assertThat(row(panel, "TRANSFERS", Platform.MOBILE).status()).isEqualTo(ServiceHealthStatus.DEGRADED);
        assertThat(row(panel, "CARDS", Platform.MOBILE).status()).isEqualTo(ServiceHealthStatus.OPERATIONAL);
    }

    @Test
    void aBranchIncidentAndAnUnknownServiceDoNotCreateRows()
    {
        signals.incidents.add(new IncidentHit(Platform.BRANCH, "PAYMENTS", 9L));
        signals.counts.add(new CustomerCount(Platform.MOBILE, "SOMETHING_ELSE", 20));

        assertThat(service.panel().services()).hasSize(8);
        assertThat(service.panel().services()).allMatch(row -> row.status() == ServiceHealthStatus.OPERATIONAL);
    }

    @Test
    void aMonitorThatHasNotRunRecentlyMarksQuietRowsUnknown()
    {
        finishedAt(NOW.minus(ServiceHealthService.STALE_AFTER).minusSeconds(1));

        ServiceHealthResponse panel = service.panel();

        assertThat(panel.dataStale()).isTrue();
        assertThat(panel.services()).allMatch(row -> row.status() == ServiceHealthStatus.UNKNOWN);
    }

    @Test
    void downRowsAreListedBeforeOperationalOnes()
    {
        signals.incidents.add(new IncidentHit(Platform.WEB, "TRANSFERS", 3L));

        assertThat(service.panel().services().get(0).service()).isEqualTo("TRANSFERS");
        assertThat(service.panel().services().get(0).status()).isEqualTo(ServiceHealthStatus.DOWN);
    }

    private void finishedAt(Instant at)
    {
        MonitorJobRun run = new MonitorJobRun(JobTrigger.SCHEDULE, at);
        run.finish(at);
        when(monitorRuns.findFirstByOrderByStartedAtDescIdDesc()).thenReturn(Optional.of(run));
    }

    private static ServiceHealthRow row(ServiceHealthResponse panel, String service, Platform platform)
    {
        return panel.services().stream()
                .filter(candidate -> candidate.service().equals(service) && candidate.platform() == platform)
                .findFirst()
                .orElseThrow();
    }

    private static final class FakeSignals implements ServiceHealthSignals
    {
        private final List<IncidentHit> incidents = new ArrayList<>();
        private final List<CustomerCount> counts = new ArrayList<>();
        private final List<NoticeHit> notices = new ArrayList<>();

        @Override
        public List<IncidentHit> openIncidentsWithOpenTickets()
        {
            return incidents;
        }

        @Override
        public List<CustomerCount> distinctCustomersSince(Instant from)
        {
            return counts;
        }

        @Override
        public List<NoticeHit> openAutoTicketNotices()
        {
            return notices;
        }
    }
}
