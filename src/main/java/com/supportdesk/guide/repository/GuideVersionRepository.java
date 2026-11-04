package com.supportdesk.guide.repository;

import com.supportdesk.guide.entity.GuideVersion;
import com.supportdesk.guide.enums.GuideVersionState;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuideVersionRepository extends JpaRepository<GuideVersion, Long>
{
    // shortcut: matching runs in Java over every live guide; move it into SQL once the library passes a few hundred guides.
    @EntityGraph(attributePaths = {"guide", "guide.platforms", "guide.errorCodes"})
    List<GuideVersion> findByState(GuideVersionState state);

    Optional<GuideVersion> findByGuideIdAndState(Long guideId, GuideVersionState state);
}
