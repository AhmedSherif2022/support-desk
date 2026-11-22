package com.supportdesk.monitor.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.monitor.entity.MonitorJobRun;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.enums.JobTrigger;
import com.supportdesk.monitor.repository.MonitorJobRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class MonitorJobRunServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private final MonitorJobRunRepository runRepository = mock(MonitorJobRunRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private MonitorJobRunService service;

    @BeforeEach
    void setUp()
    {
        service = new MonitorJobRunService(runRepository, auditService, Clock.fixed(NOW, ZoneOffset.UTC));
        when(runRepository.save(any(MonitorJobRun.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void startSavesARunThatIsStillRunning()
    {
        MonitorJobRun run = service.start(JobTrigger.SCHEDULE);

        assertThat(run.getTrigger()).isEqualTo(JobTrigger.SCHEDULE);
        assertThat(run.getStartedAt()).isEqualTo(NOW);
        assertThat(run.isRunning()).isTrue();
        verify(runRepository).save(run);
    }

    @Test
    @SuppressWarnings("unchecked")
    void finishSavesTheNumbersAndAuditsTheRun()
    {
        MonitorJobRun run = new MonitorJobRun(JobTrigger.MANUAL, NOW);
        run.count(EventOutcome.CREATED);
        run.count(EventOutcome.CREATED);
        run.count(EventOutcome.DUPLICATE);

        MonitorJobRun finished = service.finish(run);

        assertThat(finished.isRunning()).isFalse();
        ArgumentCaptor<Map<String, Object>> after = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordForCurrentUser(eq(AuditAction.MONITOR_JOB_RUN), anyString(), anyString(), isNull(), after.capture());
        assertThat(after.getValue())
                .containsEntry("trigger", "MANUAL")
                .containsEntry("rowsRead", 3)
                .containsEntry("created", 2)
                .containsEntry("duplicates", 1)
                .doesNotContainKey("error");
    }

    @Test
    @SuppressWarnings("unchecked")
    void failKeepsTheNumbersSoFarAndTheReason()
    {
        MonitorJobRun run = new MonitorJobRun(JobTrigger.SCHEDULE, NOW);
        run.count(EventOutcome.CREATED);

        MonitorJobRun failed = service.fail(run, "IllegalStateException: boom");

        assertThat(failed.getError()).isEqualTo("IllegalStateException: boom");
        assertThat(failed.getCreated()).isEqualTo(1);
        assertThat(failed.isRunning()).isFalse();
        ArgumentCaptor<Map<String, Object>> after = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordForCurrentUser(eq(AuditAction.MONITOR_JOB_RUN), anyString(), anyString(), isNull(), after.capture());
        assertThat(after.getValue()).containsEntry("error", "IllegalStateException: boom");
    }
}