package com.supportdesk.monitor.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.monitor.entity.MonitorJobRun;
import com.supportdesk.monitor.enums.JobTrigger;
import com.supportdesk.monitor.repository.MonitorJobRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class MonitorJobRunService
{
    private static final String ENTITY = "MONITOR_JOB";

    private final MonitorJobRunRepository runRepository;
    private final AuditService auditService;
    private final Clock clock;


    @Transactional
    public MonitorJobRun start(JobTrigger trigger)
    {
        return runRepository.save(new MonitorJobRun(trigger, clock.instant()));
    }


    @Transactional
    public MonitorJobRun finish(MonitorJobRun run)
    {
        run.finish(clock.instant());
        return saveAndAudit(run);
    }


    @Transactional
    public MonitorJobRun fail(MonitorJobRun run, String error)
    {
        run.fail(error, clock.instant());
        return saveAndAudit(run);
    }

    private MonitorJobRun saveAndAudit(MonitorJobRun run)
    {
        MonitorJobRun saved = runRepository.save(run);       // merges the numbers counted in memory
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("trigger", saved.getTrigger().name());
        after.put("rowsRead", saved.getRowsRead());
        after.put("created", saved.getCreated());
        after.put("duplicates", saved.getDuplicates());
        after.put("grouped", saved.getGrouped());
        after.put("unknownCodes", saved.getUnknownCodes());
        after.put("failed", saved.getFailed());
        if (saved.getError() != null) {
            after.put("error", saved.getError());
        }
        auditService.recordForCurrentUser(AuditAction.MONITOR_JOB_RUN, ENTITY, String.valueOf(saved.getId()), null, after);
        return saved;
    }
}
