package com.supportdesk.monitor.service;
import com.supportdesk.incident.service.IncidentGroupingService;
import com.supportdesk.monitor.config.MonitorProperties;
import com.supportdesk.monitor.entity.MonitorJobRun;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.enums.JobTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorJob
{
    private final MonitorRowWorker rowWorker;
    private final MonitorJobRunService runService;
    private final IncidentGroupingService groupingService;
    private final MonitorProperties props;


    @Scheduled(fixedDelayString = "${supportdesk.monitor.interval:1m}")
    public void scheduledRun()
    {
        if (props.enabled()) {
            run(JobTrigger.SCHEDULE);
        }
    }


    public MonitorJobRun run(JobTrigger trigger)
    {
        MonitorJobRun run = runService.start(trigger);
        try {
            for (int i = 0; i < props.batchSize(); i++) {
                Optional<EventOutcome> outcome = rowWorker.processNext();
                if (outcome.isEmpty()) {
                    break;                                  // nothing left
                }
                run.count(outcome.get());
            }
            // E6-S3: once the batch is handled, group mass failures into incidents (its own transaction).
            int grouped = groupingService.groupRecentFailures();
            for (int i = 0; i < grouped; i++) {
                run.countGrouped();
            }
            return runService.finish(run);
        } catch (RuntimeException e) {

            log.error("Monitor job run {} failed", run.getId(), e);
            return runService.fail(run, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}