package com.supportdesk.azure.repository;

import com.supportdesk.azure.entity.Escalation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscalationRepository extends JpaRepository<Escalation, Long>
{
    @EntityGraph(attributePaths = {"ticket", "escalatedBy"})
    Optional<Escalation> findByTicketId(Long ticketId);

    boolean existsByTicketId(Long ticketId);

    /** Member 2: the webhook finds the escalation by its Azure work item. */
    Optional<Escalation> findByWorkItemId(Long workItemId);
}
