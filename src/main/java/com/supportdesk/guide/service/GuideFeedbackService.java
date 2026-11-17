package com.supportdesk.guide.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.guide.dto.GuideDtos.FlagStepRequest;
import com.supportdesk.guide.dto.GuideDtos.GuideFeedbackDto;
import com.supportdesk.guide.dto.GuideMapper;
import com.supportdesk.guide.entity.GuideFeedback;
import com.supportdesk.guide.entity.GuideRun;
import com.supportdesk.guide.entity.GuideTree.AtStep;
import com.supportdesk.guide.entity.GuideTree.Taken;
import com.supportdesk.guide.entity.GuideTree.Walk;
import com.supportdesk.guide.exception.GuideRunNotFoundException;
import com.supportdesk.guide.repository.GuideFeedbackRepository;
import com.supportdesk.guide.repository.GuideRunRepository;
import com.supportdesk.ticket.listener.TicketAuditListener;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** E4-S5: an agent flags a step of a finished run; the guide admin reads the reports newest first. */
@Service
@RequiredArgsConstructor
public class GuideFeedbackService
{
    // Plain columns only: a path like run.ticket.customer.nationalId would order rows by customer data (E1-S2).
    private static final Set<String> SORTABLE = Set.of("createdAt", "id", "status");

    private final GuideRunRepository runs;
    private final GuideFeedbackRepository feedback;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional   // the report and GUIDE_STEP_FLAGGED commit together
    public GuideFeedbackDto flag(Long runId, FlagStepRequest request)
    {
        GuideRun run = runs.findById(runId).orElseThrow(() -> new GuideRunNotFoundException(runId));
        if (run.isActive()) {
            throw new BusinessRuleException(ErrorCode.GUIDE_RUN_STILL_ACTIVE,
                    "Guide run " + runId + " is still in progress. Finish it before flagging a step.");
        }
        // A step replayed from the run's answers on its pinned version, or the step an abandoned run stopped on:
        // the unclear step is often the one the agent never answered.
        String stepKey = request.stepKey();
        Walk walk = run.walk();
        Taken taken = walk.taken().stream().filter(step -> step.stepKey().equals(stepKey)).findFirst().orElse(null);
        AtStep stoppedAt = taken == null && walk.position() instanceof AtStep at && at.key().equals(stepKey) ? at : null;
        if (taken == null && stoppedAt == null) {
            throw new BusinessRuleException(ErrorCode.GUIDE_STEP_NOT_TAKEN,
                    "Step '" + stepKey + "' was not taken during guide run " + runId + ", and the run did not stop there.");
        }

        User agent = currentUserService.getCurrentUserEntity();     // never the client's idea of who flagged it
        GuideFeedback saved = feedback.save(new GuideFeedback(run, stepKey, agent, request.comment()));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("feedbackId", saved.getId());
        details.put("guideRunId", run.getId());
        details.put("guideId", run.getPinnedVersion().getGuide().getId());
        details.put("guideTitle", run.getPinnedVersion().getGuide().getTitle());
        details.put("version", run.getPinnedVersion().getVersionNumber());
        details.put("stepKey", stepKey);
        details.put("question", taken != null ? taken.step().question() : stoppedAt.step().question());
        if (taken != null) {
            details.put("answer", taken.answer().key());
        }
        // The comment stays out of the audit row: it is free text, kept once in guide_feedback.
        auditService.recordForCurrentUser(AuditAction.GUIDE_STEP_FLAGGED, TicketAuditListener.ENTITY,
                String.valueOf(run.getTicket().getTicketNumber()), null, details);

        return GuideMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<GuideFeedbackDto> queue(Pageable pageable)
    {
        pageable.getSort().stream().filter(order -> !SORTABLE.contains(order.getProperty())).findFirst()
                .ifPresent(order -> {
                    throw new ApiException(ErrorCode.REQUEST_ERROR, "Feedback cannot be sorted by " + order.getProperty() + ".");
                });
        return feedback.findAllBy(pageable).map(GuideMapper::toDto);
    }
}
