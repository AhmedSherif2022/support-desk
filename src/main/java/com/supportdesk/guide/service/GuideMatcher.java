package com.supportdesk.guide.service;

import com.supportdesk.customer.enums.Platform;
import com.supportdesk.guide.dto.GuideMatch;
import com.supportdesk.guide.entity.Guide;
import com.supportdesk.guide.entity.GuideVersion;
import com.supportdesk.guide.enums.GuideVersionState;
import com.supportdesk.guide.repository.GuideVersionRepository;
import com.supportdesk.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class GuideMatcher
{
    private final GuideVersionRepository versions;

    @Transactional(readOnly = true)
    public List<GuideMatch> matchesFor(Ticket ticket)
    {
        return versions.findByState(GuideVersionState.LIVE).stream()
                .flatMap(version -> version.getGuide().matchFor(ticket)
                        .map(type -> new GuideMatch(version, type)).stream())
                .sorted(Comparator.comparing(GuideMatch::matchedBy)
                        .thenComparing(match -> match.version().getGuide().getTitle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Predicate<Ticket> loadAvailability()
    {
        List<Guide> live = versions.findByState(GuideVersionState.LIVE).stream().map(GuideVersion::getGuide).toList();
        return ticket -> live.stream().anyMatch(guide -> guide.matchFor(ticket).isPresent());
    }

    /** E2-S4: the live guide for an error seen in the customer's logs on one of these platforms; first by title. */
    @Transactional(readOnly = true)
    public Optional<GuideVersion> forError(String errorCode, Set<Platform> platforms)
    {
        return versions.findByState(GuideVersionState.LIVE).stream()
                .filter(version -> version.getGuide().getErrorCodes().contains(errorCode)
                        && version.getGuide().getPlatforms().stream().anyMatch(platforms::contains))
                .min(Comparator.comparing(version -> version.getGuide().getTitle()));
    }

    public boolean hasGuideFor(Ticket ticket)
    {
        return loadAvailability().test(ticket);
    }
}
