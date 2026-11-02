package com.supportdesk.ticket.controller;

import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.ticket.enums.QueueChip;
import com.supportdesk.ticket.enums.TicketListView;
import com.supportdesk.ticket.dto.QueueResponse;
import com.supportdesk.ticket.dto.TicketDtos.ChangeStatusRequest;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckDto;
import com.supportdesk.ticket.dto.TicketDtos.PrecheckRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactDto;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactRequest;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactsResponse;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.HistoryResponse;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.TicketListRow;
import com.supportdesk.ticket.service.TicketListService;
import com.supportdesk.ticket.service.TicketQueryService;
import com.supportdesk.ticket.service.TicketService;
import com.supportdesk.ticket.service.TicketWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class TicketController implements TicketApi
{
    private final TicketService ticketService;
    private final TicketQueryService ticketQueryService;
    private final TicketListService ticketListService;
    private final TicketWorkspaceService workspaceService;

    @Override
    public TicketDto create(CreateTicketRequest request)
    {
        return ticketService.create(request);
    }

    @Override
    public PrecheckDto precheck(PrecheckRequest request)
    {
        return ticketService.precheck(request);
    }

    @Override
    public QueueResponse queue(QueueChip chip)
    {
        return ticketQueryService.queue(chip);
    }

    @Override
    public PageResponse<TicketListRow> list(TicketListView view, String q, Pageable pageable)
    {
        return ticketListService.list(view, q, pageable);
    }

    @Override
    public TicketDto get(Long number)
    {
        return ticketService.get(number);
    }

    @Override
    public TicketDto changeStatus(Long number, ChangeStatusRequest request)
    {
        return ticketService.changeStatus(number, request);
    }

    @Override
    public ContactDto addContact(Long number, ContactRequest request)
    {
        return workspaceService.addContact(number, request.note());
    }

    @Override
    public ContactsResponse contacts(Long number)
    {
        return workspaceService.contacts(number);
    }

    @Override
    public HistoryResponse timeline(Long number)
    {
        return workspaceService.timeline(number);
    }

    @Override
    public HistoryResponse audit(Long number)
    {
        return workspaceService.audit(number);
    }
}
