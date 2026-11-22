package com.supportdesk.monitor.service;
import com.supportdesk.monitor.config.MonitorProperties;
import com.supportdesk.monitor.entity.MonitorJobRun;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.enums.JobTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MonitorJobTest
{
    private final MonitorRowWorker rowWorker = mock(MonitorRowWorker.class);
    private final MonitorJobRunService runService = mock(MonitorJobRunService.class);
    private MonitorJob job;
    private MonitorJobRun run;

    @BeforeEach
    void setUp()
    {
        job = new MonitorJob(rowWorker, runService, new MonitorProperties(true, 100, Duration.ofHours(24)));
        run = new MonitorJobRun(JobTrigger.MANUAL, Instant.parse("2026-11-22T10:00:00Z"));
        when(runService.start(JobTrigger.MANUAL)).thenReturn(run);
        when(runService.finish(any())).thenAnswer(i -> i.getArgument(0));
        when(runService.fail(any(), anyString())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void countsEveryOutcomeAndStopsWhenNothingIsLeft()
    {
        when(rowWorker.processNext()).thenReturn(Optional.of(EventOutcome.CREATED),
                Optional.of(EventOutcome.DUPLICATE), Optional.of(EventOutcome.UNKNOWN_CODE), Optional.empty());

        MonitorJobRun result = job.run(JobTrigger.MANUAL);

        assertThat(result.getRowsRead()).isEqualTo(3);
        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getDuplicates()).isEqualTo(1);
        assertThat(result.getUnknownCodes()).isEqualTo(1);
        verify(runService).finish(run);
    }

    @Test
    void aFailingRowStopsTheRunAndKeepsTheNumbersSoFar()
    {
        when(rowWorker.processNext()).thenReturn(Optional.of(EventOutcome.CREATED))
                .thenThrow(new IllegalStateException("boom"));

        MonitorJobRun result = job.run(JobTrigger.MANUAL);

        assertThat(result.getCreated()).isEqualTo(1);
        verify(runService).fail(eq(run), contains("boom"));
        verify(runService, never()).finish(any());
    }
}
