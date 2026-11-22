package com.supportdesk.monitor.entity;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.enums.JobTrigger;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Table(name = "monitor_job_run")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitorJobRun
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger", nullable = false, length = 10)
    private JobTrigger trigger;                       // SCHEDULE or MANUAL

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;                       // null while running

    @Column(name = "rows_read", nullable = false)
    private int rowsRead;

    @Column(nullable = false)
    private int created;

    @Column(nullable = false)
    private int duplicates;

    @Column(nullable = false)
    private int grouped;

    @Column(name = "unknown_codes", nullable = false)
    private int unknownCodes;

    @Column(nullable = false)
    private int failed;                               // rows that threw and were set aside as FAILED

    @Column(length = 1000)
    private String error;                             // set when the run failed

    public MonitorJobRun(JobTrigger trigger, Instant now)
    {
        this.trigger = trigger;
        this.startedAt = now;
    }


    public void count(EventOutcome outcome)
    {
        rowsRead++;
        switch (outcome) {
            case CREATED -> created++;
            case DUPLICATE -> duplicates++;
            case GROUPED -> grouped++;
            case UNKNOWN_CODE -> unknownCodes++;
            case FAILED -> failed++;
        }
    }


    public void countGrouped()
    {
        grouped++;
    }

    public void finish(Instant now)
    {
        this.finishedAt = now;
    }


    public void fail(String error, Instant now)
    {
        this.error = error == null ? null : (error.length() <= 1000 ? error : error.substring(0, 1000));
        this.finishedAt = now;
    }

    public boolean isRunning()
    {
        return finishedAt == null;
    }
}
