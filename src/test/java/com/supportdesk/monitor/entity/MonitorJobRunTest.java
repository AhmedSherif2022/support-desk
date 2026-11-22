package com.supportdesk.monitor.entity;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.enums.JobTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorJobRunTest
{
    private static final Instant START = Instant.parse("2026-11-22T10:00:00Z");

    private MonitorJobRun run;

    @BeforeEach
    void setUp()
    {
        run = new MonitorJobRun(JobTrigger.SCHEDULE, START);
    }

    @Test
    void newRunIsRunningWithZeroNumbers()
    {
        assertThat(run.isRunning()).isTrue();
        assertThat(run.getStartedAt()).isEqualTo(START);
        assertThat(run.getRowsRead()).isZero();
        assertThat(run.getCreated()).isZero();
    }

    @Test
    void eachOutcomeGoesToItsOwnCounter()
    {
        run.count(EventOutcome.CREATED);
        run.count(EventOutcome.CREATED);
        run.count(EventOutcome.DUPLICATE);
        run.count(EventOutcome.GROUPED);
        run.count(EventOutcome.UNKNOWN_CODE);

        assertThat(run.getRowsRead()).isEqualTo(5);      // every row counts as read
        assertThat(run.getCreated()).isEqualTo(2);
        assertThat(run.getDuplicates()).isEqualTo(1);
        assertThat(run.getGrouped()).isEqualTo(1);
        assertThat(run.getUnknownCodes()).isEqualTo(1);
    }

    @Test
    void finishStopsTheRun()
    {
        run.finish(START.plusSeconds(3));

        assertThat(run.isRunning()).isFalse();
        assertThat(run.getFinishedAt()).isEqualTo(START.plusSeconds(3));
        assertThat(run.getError()).isNull();
    }

    @Test
    void failKeepsTheNumbersAndTheReason()
    {
        run.count(EventOutcome.CREATED);

        run.fail("IllegalStateException: boom", START.plusSeconds(2));

        assertThat(run.isRunning()).isFalse();
        assertThat(run.getCreated()).isEqualTo(1);
        assertThat(run.getError()).isEqualTo("IllegalStateException: boom");
    }

    @Test
    void aVeryLongErrorIsCutToFitTheColumn()
    {
        run.fail("x".repeat(1500), START);

        assertThat(run.getError()).hasSize(1000);        // error VARCHAR(1000)
    }
}
