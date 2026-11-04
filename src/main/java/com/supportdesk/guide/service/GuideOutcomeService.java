package com.supportdesk.guide.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.guide.dto.GuideDtos.CompleteRunRequest;
import com.supportdesk.guide.dto.GuideDtos.CompletedRunDto;
import com.supportdesk.guide.dto.GuideMapper;
import com.supportdesk.guide.entity.GuideRun;
import com.supportdesk.guide.entity.GuideTree.AtOutcome;
import com.supportdesk.guide.entity.GuideTree.Outcome;
import com.supportdesk.guide.entity.GuideTree.Walk;
import com.supportdesk.guide.enums.GuideRunStatus;
import com.supportdesk.guide.exception.GuideOutcomeNotReachedException;
import com.supportdesk.guide.exception.GuideRunNotFoundException;
import com.supportdesk.guide.repository.GuideRunRepository;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.listener.TicketAuditListener;
import com.supportdesk.ticket.service.SlaService;
import com.supportdesk.ticket.service.TicketService;
import com.supportdesk.ticket.service.TicketWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class GuideOutcomeService
{
    private final GuideRunRepository runs;
    private final TicketService ticketService;
    private final SlaService slaService;
    private final TicketWorkspaceService workspaceService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public CompletedRunDto complete(Long runId, CompleteRunRequest request)
    {

        GuideRun run = runs.findLockedById(runId).orElseThrow(() -> new GuideRunNotFoundException(runId));


        if (run.status() != GuideRunStatus.OUTCOME_REACHED) {
            throw new GuideOutcomeNotReachedException(runId);
        }


        Walk walk = run.walk();
        AtOutcome at = (AtOutcome) walk.position();
        Outcome outcome = at.outcome();
        Ticket ticket = run.getTicket();
        User agent = currentUserService.getCurrentUserEntity();
        Instant now = clock.instant();


        switch (outcome.type()) {
            case RESOLVED -> ticketService.moveStatus(ticket, TicketStatus.RESOLVED, agent);
            case FOLLOW_UP -> {
                ticketService.moveStatus(ticket, TicketStatus.WAITING_ON_CUSTOMER, agent);
                // SLA clock stops while we wait for the customer (see the note below about pause).
                if (!ticket.isSlaPaused()) {
                    slaService.pause(ticket);
                }
            }
            case ESCALATE ->
                // TODO (E5 Azure DevOps): open the escalation dialog and create the work item with this run's path.
                    ticketService.moveStatus(ticket, TicketStatus.ESCALATED, agent);
        }


        run.complete(at.key(), outcome.type(), request == null ? null : request.notes(), now);


        auditService.recordForCurrentUser(AuditAction.GUIDE_COMPLETED, TicketAuditListener.ENTITY,
                String.valueOf(ticket.getTicketNumber()), null, summary(run, walk, at, now));


        workspaceService.logCustomerScript(ticket, agent, outcome.customerScript());


        return new CompletedRunDto(GuideMapper.toDto(run), outcome.type(), ticket.getStatus(), outcome.customerScript());
    }


    private static Map<String, Object> summary(GuideRun run, Walk walk, AtOutcome at, Instant now)
    {
        List<Map<String, String>> path = walk.taken().stream()
                .map(taken -> Map.of("step", taken.stepKey(), "answer", taken.answer().key()))
                .toList();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("guideRunId", run.getId());
        details.put("guideId", run.getPinnedVersion().getGuide().getId());
        details.put("guideTitle", run.getPinnedVersion().getGuide().getTitle());
        details.put("version", run.getPinnedVersion().getVersionNumber());
        details.put("path", path);
        details.put("durationSeconds", Duration.between(run.getStartedAt(), now).toSeconds());
        details.put("outcome", at.key());
        details.put("outcomeType", at.outcome().type().name());
        return details;
    }
}



