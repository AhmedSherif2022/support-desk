package com.supportdesk.monitor.controller;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.monitor.dto.MonitorDtos.JobRunDto;
import com.supportdesk.monitor.enums.JobTrigger;
import com.supportdesk.monitor.repository.MonitorJobRunRepository;
import com.supportdesk.monitor.service.MonitorJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController                              // Spring: handles HTTP requests and returns JSON
@PreAuthorize("hasRole('GUIDE_ADMIN')")      // Spring Security: 403 for every other role
@RequiredArgsConstructor
public class MonitorController implements MonitorApi
{
    private final MonitorJob monitorJob;
    private final MonitorJobRunRepository runRepository;

    @Override
    public JobRunDto runNow()
    {
        return JobRunDto.from(monitorJob.run(JobTrigger.MANUAL));
    }

    @Override
    public PageResponse<JobRunDto> runs(Pageable pageable)
    {
        return PageResponse.of(runRepository.findAllByOrderByStartedAtDescIdDesc(pageable).map(JobRunDto::from));
    }
}
