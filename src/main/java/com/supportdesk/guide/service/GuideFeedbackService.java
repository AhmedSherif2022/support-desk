package com.supportdesk.guide.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.guide.dto.GuideDtos.FlagStepRequest;
import com.supportdesk.guide.dto.GuideDtos.GuideFeedbackDto;
import com.supportdesk.guide.dto.GuideMapper;
import com.supportdesk.guide.entity.GuideFeedback;
import com.supportdesk.guide.entity.GuideRun;
import com.supportdesk.guide.entity.GuideTree.Taken;
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

/** E4-S5: an agent flags a step of a finished run; the guide admin reads the reports newest first. */
@Service
@RequiredArgsConstructor
public class GuideFeedbackService
{
    private final GuideRunRepository runs;
    private final GuideFeedbackRepository feedback;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional   // the report and GUIDE_STEP_FLAGGED commit together
    public GuideFeedbackDto flag(Long runId, FlagStepRequest request)
    {
        GuideRun run = runs.findById(runId).orElseThrow(() -> new GuideRunNotFoundException(runId));
        if (run.isActive()) {
            throw new BusinessRuleException(ErrorCode.GUIDE_RUN_NOT_COMPLETED,
                    "Guide run " + runId + " is still in progress. Finish it before flagging a step.");
        }
        // Only the steps replayed from the run's answers on its pinned version count as taken.
        Taken taken = run.walk().taken().stream()
                .filter(step -> step.stepKey().equals(request.stepKey()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.GUIDE_STEP_NOT_TAKEN,
                        "Step '" + request.stepKey() + "' was not taken during guide run " + runId + "."));

        User agent = currentUserService.getCurrentUserEntity();     // never the client's idea of who flagged it
        GuideFeedback saved = feedback.save(new GuideFeedback(run, taken.stepKey(), agent, request.comment()));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("feedbackId", saved.getId());
        details.put("guideRunId", run.getId());
        details.put("guideId", run.getPinnedVersion().getGuide().getId());
        details.put("guideTitle", run.getPinnedVersion().getGuide().getTitle());
        details.put("version", run.getPinnedVersion().getVersionNumber());
        details.put("stepKey", taken.stepKey());
        details.put("question", taken.step().question());
        details.put("answer", taken.answer().key());
        details.put("comment", saved.getComment());
        details.put("status", saved.getStatus().name());
        auditService.recordForCurrentUser(AuditAction.GUIDE_STEP_FLAGGED, TicketAuditListener.ENTITY,
                String.valueOf(run.getTicket().getTicketNumber()), null, details);

        return GuideMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<GuideFeedbackDto> queue(Pageable pageable)
    {
        return feedback.findAllBy(pageable).map(GuideMapper::toDto);
    }
}
