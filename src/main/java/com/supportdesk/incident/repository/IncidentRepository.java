package com.supportdesk.incident.repository;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.incident.entity.Incident;
import com.supportdesk.incident.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long>
{
    /** The open incident for one failure group; the partial unique index guarantees there is at most one. */
    Optional<Incident> findByErrorCodeAndPlatformAndStatus(String errorCode, Platform platform, IncidentStatus status);

    Page<Incident> findByStatusOrderByCreatedAtDescIdDesc(IncidentStatus status, Pageable pageable);

    Page<Incident> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
