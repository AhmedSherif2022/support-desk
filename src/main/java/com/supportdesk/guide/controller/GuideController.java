package com.supportdesk.guide.controller;

import com.supportdesk.guide.dto.GuideDtos.RecommendedGuideDto;
import com.supportdesk.guide.service.GuideRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class GuideController implements GuideApi
{
    private final GuideRunService guideRunService;

    @Override
    public List<RecommendedGuideDto> recommended(Long ticketNumber)
    {
        return guideRunService.recommend(ticketNumber);
    }
}
