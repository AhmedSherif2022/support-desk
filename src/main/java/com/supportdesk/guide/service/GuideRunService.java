package com.supportdesk.guide.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.guide.dto.GuideDtos.AnswerRequest;
import com.supportdesk.guide.dto.GuideDtos.GuideRunDto;
import com.supportdesk.guide.dto.GuideDtos.RecommendedGuideDto;
import com.supportdesk.guide.dto.GuideDtos.StartGuideRunRequest;
import com.supportdesk.guide.dto.GuideMapper;
import com.supportdesk.guide.entity.GuideRun;
import com.supportdesk.guide.entity.GuideVersion;
import com.supportdesk.guide.enums.GuideVersionState;
import com.supportdesk.guide.exception.GuideNotFoundException;
import com.supportdesk.guide.exception.GuideRunNotFoundException;
import com.supportdesk.guide.repository.GuideRunRepository;
import com.supportdesk.guide.repository.GuideVersionRepository;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.listener.TicketAuditListener;
import com.supportdesk.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GuideRunService
{
    private final TicketService ticketService;
    private final GuideMatcher guideMatcher;
    private final GuideVersionRepository versions;
    private final GuideRunRepository runs;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<RecommendedGuideDto> recommend(Long ticketNumber)
    {
        Ticket ticket = ticketService.getEntity(ticketNumber);
        return guideMatcher.matchesFor(ticket).stream().map(match -> GuideMapper.toDto(match, ticket)).toList();
    }

    @Transactional   // the run, the move to Troubleshooting and GUIDE_STARTED commit together
    public GuideRunDto start(StartGuideRunRequest request)
    {
        Ticket ticket = ticketService.getEntity(request.ticketNumber());
        GuideVersion version = versions.findByGuideIdAndState(request.guideId(), GuideVersionState.LIVE)
                .orElseThrow(() -> new GuideNotFoundException(request.guideId()));
        if (version.getGuide().matchFor(ticket).isEmpty()) {
            throw new BusinessRuleException(ErrorCode.GUIDE_DOES_NOT_FIT,
                    "'" + version.getGuide().getTitle() + "' does not fit ticket " + ticket.displayNumber() + ".");
        }
        runs.findByTicketIdAndCompletedAtIsNull(ticket.getId()).ifPresent(active -> {
            if (ticket.getStatus() == TicketStatus.TROUBLESHOOTING) {
                throw new BusinessRuleException(ErrorCode.GUIDE_RUN_ALREADY_ACTIVE,
                        "Ticket " + ticket.displayNumber() + " already has guide run " + active.getId() + " in progress.");
            }
            // The ticket was moved on by hand (resolved, then reopened). Flush so the one-active-run index frees up.
            active.abandon(clock.instant());
            runs.flush();
        });

        User agent = currentUserService.getCurrentUserEntity();
        if (ticket.getStatus() != TicketStatus.TROUBLESHOOTING) {
            ticketService.moveStatus(ticket, TicketStatus.TROUBLESHOOTING, agent);
        }
        GuideRun run = runs.save(new GuideRun(ticket, version, agent, clock.instant()));
        auditService.recordForCurrentUser(AuditAction.GUIDE_STARTED, TicketAuditListener.ENTITY,
                String.valueOf(ticket.getTicketNumber()), null, Map.of(
                        "guideRunId", run.getId(),
                        "guideId", version.getGuide().getId(),
                        "guideTitle", version.getGuide().getTitle(),
                        "version", version.getVersionNumber()));
        return GuideMapper.toDto(run);
    }

    @Transactional(readOnly = true)
    public GuideRunDto get(Long runId)
    {
        return GuideMapper.toDto(runs.findById(runId).orElseThrow(() -> new GuideRunNotFoundException(runId)));
    }

    @Transactional
    public GuideRunDto answer(Long runId, AnswerRequest request)
    {
        GuideRun run = lockActive(runId);
        run.answer(request.stepKey(), request.answerKey(), clock.instant());
        return GuideMapper.toDto(run);
    }

    @Transactional
    public GuideRunDto clearFrom(Long runId, int position)
    {
        if (position < 1) {
            throw new BusinessRuleException(ErrorCode.INVALID_GUIDE_ROUTE, "Answer positions start at 1.");
        }
        GuideRun run = lockActive(runId);
        run.clearFrom(position);
        return GuideMapper.toDto(run);
    }

    private GuideRun lockActive(Long runId)
    {
        GuideRun run = runs.findLockedById(runId).orElseThrow(() -> new GuideRunNotFoundException(runId));
        if (!run.isActive()) {
            throw new BusinessRuleException(ErrorCode.GUIDE_RUN_NOT_ACTIVE);
        }
        return run;
    }
}
