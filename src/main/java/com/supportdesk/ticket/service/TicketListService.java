package com.supportdesk.ticket.service;

import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.ticket.enums.TicketListView;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.TicketListRow;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The agent's ticket list (E3-S7): their own tickets, filtered by status and searched by ticket number or CIF. */
@Service
@RequiredArgsConstructor
public class TicketListService
{
    private static final List<TicketStatus> CLOSED_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PageResponse<TicketListRow> list(TicketListView view, String q, Pageable pageable)
    {
        Long agentId = currentUserService.getCurrentUserId();
        Specification<Ticket> spec = Specification.<Ticket>where(
                        (root, query, cb) -> cb.equal(root.get("assignedAgent").get("id"), agentId))
                .and(status(view == null ? TicketListView.ALL : view))
                .and(search(q));
        return PageResponse.of(ticketRepository.findAll(spec, pageable).map(TicketListService::toRow));
    }

    private static Specification<Ticket> status(TicketListView view)
    {
        return switch (view) {
            case ALL -> null;
            case OPEN -> (root, query, cb) -> cb.not(root.get("status").in(CLOSED_STATUSES));
            case RESOLVED_CLOSED -> (root, query, cb) -> root.get("status").in(CLOSED_STATUSES);
        };
    }

    /** "#2001" / "2001" match the ticket number; anything else matches the customer's CIF. */
    private static Specification<Ticket> search(String q)
    {
        if (q == null || q.isBlank()) {
            return null;
        }
        String text = q.trim();
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.matches("\\d{1,9}")) {
            long number = Long.parseLong(digits);
            return (root, query, cb) -> cb.equal(root.get("ticketNumber"), number);
        }
        // TODO (E5 escalation): also match Azure DevOps work item numbers once tickets store them.
        return (root, query, cb) -> cb.equal(cb.upper(root.get("customer").get("cif")), text.toUpperCase());
    }

    private static TicketListRow toRow(Ticket t)
    {
        return new TicketListRow(t.displayNumber(), t.getTitle(), t.getCustomer().getCif(), t.getCustomer().getFullName(),
                t.getStatus(), t.getPriority(), t.getOrigin(), t.getSlaState(), t.getCreatedAt());
    }
}
