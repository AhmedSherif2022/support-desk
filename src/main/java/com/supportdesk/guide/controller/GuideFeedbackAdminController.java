package com.supportdesk.guide.controller;

import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.guide.dto.GuideDtos.GuideFeedbackDto;
import com.supportdesk.guide.service.GuideFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('GUIDE_ADMIN')")
@RequiredArgsConstructor
public class GuideFeedbackAdminController implements GuideFeedbackAdminApi
{
    private final GuideFeedbackService guideFeedbackService;

    @Override
    public PageResponse<GuideFeedbackDto> queue(Pageable pageable)
    {
        return PageResponse.of(guideFeedbackService.queue(pageable));
    }
}
