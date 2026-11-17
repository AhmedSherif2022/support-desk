package com.supportdesk.guide.controller;
import com.supportdesk.guide.dto.GuideDtos.AnswerRequest;
import com.supportdesk.guide.dto.GuideDtos.CompleteRunRequest;
import com.supportdesk.guide.dto.GuideDtos.CompletedRunDto;
import com.supportdesk.guide.dto.GuideDtos.FlagStepRequest;
import com.supportdesk.guide.dto.GuideDtos.GuideFeedbackDto;
import com.supportdesk.guide.dto.GuideDtos.GuideRunDto;
import com.supportdesk.guide.dto.GuideDtos.StartGuideRunRequest;
import com.supportdesk.guide.service.GuideFeedbackService;
import com.supportdesk.guide.service.GuideOutcomeService;
import com.supportdesk.guide.service.GuideRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class GuideRunController implements GuideRunApi
{
    private final GuideRunService guideRunService;
    private final GuideOutcomeService guideOutcomeService;
    private final GuideFeedbackService guideFeedbackService;

    @Override
    public GuideRunDto start(StartGuideRunRequest request)
    {
        return guideRunService.start(request);
    }

    @Override
    public List<GuideRunDto> forTicket(Long ticketNumber)
    {
        return guideRunService.forTicket(ticketNumber);
    }

    @Override
    public GuideRunDto get(Long id)
    {
        return guideRunService.get(id);
    }

    @Override
    public GuideRunDto answer(Long id, AnswerRequest request)
    {
        return guideRunService.answer(id, request);
    }

    @Override
    public GuideRunDto clearFrom(Long id, int position)
    {
        return guideRunService.clearFrom(id, position);
    }

    @Override
    public CompletedRunDto complete(Long id, CompleteRunRequest request)
    {
        return guideOutcomeService.complete(id, request);
    }

    @Override
    public GuideFeedbackDto flag(Long id, FlagStepRequest request)
    {
        return guideFeedbackService.flag(id, request);
    }
}
