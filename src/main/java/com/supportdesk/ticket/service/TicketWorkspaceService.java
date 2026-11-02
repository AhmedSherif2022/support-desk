package com.supportdesk.ticket.service;

import com.supportdesk.audit.dto.AuditLogResponse;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactDto;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.ContactsResponse;
import com.supportdesk.ticket.dto.TicketWorkspaceDtos.HistoryResponse;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.entity.TicketContact;
import com.supportdesk.ticket.listener.TicketAuditListener;
import com.supportdesk.ticket.repository.TicketContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ticket workspace tabs (E3-S5, E3-S6): Customer contact, Timeline and Audit, each newest first with a count.
 * Timeline and Audit are read from the append-only audit trail, so they cannot drift from what really happened.
 */
@Service
@RequiredArgsConstructor
public class TicketWorkspaceService
{
    private static final Set<AuditAction> TIMELINE = Set.of(AuditAction.TICKET_CREATED, AuditAction.TICKET_STATUS_CHANGED);

    private final TicketService ticketService;
    private final TicketContactRepository contactRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    @Transactional   // the contact, the ticket's last-contact time and CUSTOMER_CONTACTED commit together
    public ContactDto addContact(Long ticketNumber, String note)
    {
        Ticket ticket = ticketService.getEntity(ticketNumber);
        User agent = currentUserService.getCurrentUserEntity();
        Instant now = clock.instant();

        TicketContact contact = contactRepository.save(new TicketContact(ticket, agent, note.trim(), now));
        ticket.recordCustomerContact(now);
        auditService.recordForCurrentUser(AuditAction.CUSTOMER_CONTACTED, TicketAuditListener.ENTITY,
                String.valueOf(ticketNumber), null, Map.of("contactId", contact.getId()));
        // TODO (E4 guides): log a guide outcome's customer script automatically as "Told customer: …".
        return toDto(contact);
    }

    @Transactional(readOnly = true)
    public ContactsResponse contacts(Long ticketNumber)
    {
        Ticket ticket = ticketService.getEntity(ticketNumber);
        List<ContactDto> items = contactRepository.findByTicketIdOrderByCreatedAtDescIdDesc(ticket.getId()).stream()
                .map(TicketWorkspaceService::toDto)
                .toList();
        return new ContactsResponse(items.size(), items);
    }

    @Transactional(readOnly = true)
    public HistoryResponse timeline(Long ticketNumber)
    {
        return history(ticketNumber, true);
    }

    @Transactional(readOnly = true)
    public HistoryResponse audit(Long ticketNumber)
    {
        return history(ticketNumber, false);
    }

    private HistoryResponse history(Long ticketNumber, boolean timelineOnly)
    {
        ticketService.getEntity(ticketNumber);   // 404 for an unknown ticket
        List<AuditLogResponse> items = auditLogRepository
                .findByEntityAndEntityIdOrderByCreatedAtDescIdDesc(TicketAuditListener.ENTITY, String.valueOf(ticketNumber))
                .stream()
                .filter(log -> !timelineOnly || TIMELINE.contains(log.getAction()))
                .map(AuditLogResponse::from)
                .toList();
        return new HistoryResponse(items.size(), items);
    }

    private static ContactDto toDto(TicketContact contact)
    {
        return new ContactDto(contact.getId(), contact.getNote(),
                contact.getAuthor() == null ? null : contact.getAuthor().getFullName(), contact.getCreatedAt());
    }
}
