package com.supportdesk.ticket.controller;

import com.supportdesk.ticket.dto.TicketDtos.TakeStepRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import com.supportdesk.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasRole('REVIEWER')")
@RequiredArgsConstructor
public class TicketReviewController implements TicketReviewApi
{
    private final TicketService ticketService;

    @Override
    public List<TicketDto> inReview()
    {
        return ticketService.inReview();
    }

    @Override
    public TicketDto takeStep(Long number, TakeStepRequest request)
    {
        return ticketService.takeStep(number, request);
    }
}
