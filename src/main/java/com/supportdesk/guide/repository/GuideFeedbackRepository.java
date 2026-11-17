package com.supportdesk.guide.repository;

import com.supportdesk.guide.entity.GuideFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideFeedbackRepository extends JpaRepository<GuideFeedback, Long>
{
    // The admin queue shows the guide, version, agent and ticket of every row: fetch them in the one query.
    @EntityGraph(attributePaths = {"guideVersion", "guideVersion.guide", "agent", "run", "run.ticket"})
    Page<GuideFeedback> findAllBy(Pageable pageable);
}
