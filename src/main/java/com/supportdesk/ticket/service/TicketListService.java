package com.supportdesk.ticket.service;

import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import com.supportdesk.customer.service.CifFormat;
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
import java.util.Set;

/** The agent's ticket list (E3-S7): their own tickets, filtered by status and searched by ticket number or CIF. */
@Service
@RequiredArgsConstructor
public class TicketListService
{
    private static final List<TicketStatus> CLOSED_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);
    // Plain columns only: a path like customer.nationalId would order rows by a field the agent cannot see.
    private static final Set<String> SORTABLE = Set.of("createdAt", "ticketNumber", "priority", "status", "slaDueAt");

    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PageResponse<TicketListRow> list(TicketListView view, String q, Pageable pageable)
    {
        pageable.getSort().stream().filter(order -> !SORTABLE.contains(order.getProperty())).findFirst()
                .ifPresent(order -> {
                    throw new ApiException(ErrorCode.REQUEST_ERROR, "Tickets cannot be sorted by " + order.getProperty() + ".");
                });
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

    /**
     * "#2001" matches the ticket number only. A plain number ("2001", "100245") matches the ticket number
     * or the customer's CIF number (no "CIF-" needed). Anything else ("CIF-100245") matches the CIF.
     */
    static Specification<Ticket> search(String q)
    {
        if (q == null || q.isBlank()) {
            return null;
        }
        String text = q.trim();
        // TODO (E5 escalation): also match Azure DevOps work item numbers once tickets store them.
        if (text.startsWith("#")) {
            String digits = text.substring(1);
            return digits.matches("\\d{1,9}") ? byTicketNumber(Long.parseLong(digits)) : byCif(text);
        }
        Specification<Ticket> cif = byCif(CifFormat.normalize(text));
        return text.matches("\\d{1,9}") ? byTicketNumber(Long.parseLong(text)).or(cif) : cif;
    }

    private static Specification<Ticket> byTicketNumber(long number)
    {
        return (root, query, cb) -> cb.equal(root.get("ticketNumber"), number);
    }

    private static Specification<Ticket> byCif(String cif)
    {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("customer").get("cif")), cif.toUpperCase());
    }

    private static TicketListRow toRow(Ticket t)
    {
        return new TicketListRow(t.displayNumber(), t.getTitle(), t.getCustomer().getCif(), t.getCustomer().getFullName(),
                t.getStatus(), t.getPriority(), t.getOrigin(), t.getSlaState(), t.getCreatedAt());
    }
}
