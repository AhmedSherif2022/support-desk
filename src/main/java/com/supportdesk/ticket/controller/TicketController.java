package com.supportdesk.ticket.controller;
import com.supportdesk.ticket.dto.TicketDtos.ChangeStatusRequest;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckDto;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import com.supportdesk.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class TicketController
{
    private final TicketService ticketService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto create(@Valid @RequestBody CreateTicketRequest request)
    {
        return ticketService.create(request);
    }

    @PostMapping("/precheck")
    public PrecheckDto precheck(@Valid @RequestBody PrecheckRequest request)
    {
        return ticketService.precheck(request);
    }

    @GetMapping("/{number}")
    public TicketDto get(@PathVariable Long number)
    {
        return ticketService.get(number);
    }

    @PatchMapping("/{number}/status")
    public TicketDto changeStatus(@PathVariable Long number,
                                  @Valid @RequestBody ChangeStatusRequest request)
    {
        return ticketService.changeStatus(number, request);
    }
}
