package com.supportdesk.monitor.repository;
import com.supportdesk.monitor.entity.MonitorJobRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonitorJobRunRepository extends JpaRepository<MonitorJobRun, Long>
{

    Page<MonitorJobRun> findAllByOrderByStartedAtDescIdDesc(Pageable pageable);
    Optional<MonitorJobRun> findFirstByOrderByStartedAtDescIdDesc();
}