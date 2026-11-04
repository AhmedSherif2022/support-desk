package com.supportdesk.guide.repository;

import com.supportdesk.guide.entity.GuideRun;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface GuideRunRepository extends JpaRepository<GuideRun, Long>
{
    Optional<GuideRun> findByTicketIdAndCompletedAtIsNull(Long ticketId);

    // Answer and Previous rewrite the run's answers; two tabs at once would otherwise interleave them.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GuideRun> findLockedById(Long id);
}
