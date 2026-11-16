package com.supportdesk.ticket.controller;

import com.supportdesk.ticket.dto.TicketDtos.TakeStepRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import com.supportdesk.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController                                        // Spring: handles HTTP requests and returns JSON
@PreAuthorize("hasAnyRole('AGENT', 'REVIEWER')")       // Spring Security: Agent and Reviewer can review; Guide Admin gets 403
@RequiredArgsConstructor                               // Lombok: constructor for the final fields
public class TicketReviewController implements TicketReviewApi
{
    private final TicketService ticketService;

    @Override
    public List<TicketDto> inReview()
    {
        return ticketService.inReview();
    }

    @Override
    public TicketDto get(Long number)
    {
        return ticketService.get(number);              // CHECK: the same method TicketController uses to return one TicketDto
    }

    @Override
    public TicketDto takeStep(Long number, TakeStepRequest request)
    {
        return ticketService.takeStep(number, request);
    }
}
