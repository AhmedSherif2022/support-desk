package com.supportdesk.monitor.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.monitor.config.MonitorProperties;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.monitor.entity.ErrorRule;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.listener.TicketAuditListener;
import com.supportdesk.ticket.repository.TicketRepository;
import com.supportdesk.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class AutoTicketService
{
    private static final List<TicketStatus> CLOSED = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final AuditService auditService;
    private final MonitorProperties props;


    public Ticket create(DetectedErrorEvent event, ErrorRule rule, Instant now)
    {
        String description = event.getMessage() == null || event.getMessage().isBlank()
                ? "Detected by monitoring: " + event.getErrorCode()
                : "Detected by monitoring: " + event.getMessage();
        return ticketService.createSystemTicket(event.getCustomer(), rule.getCategory(), rule.getIssue(),
                description, rule.getPriority(), event.getPlatform(), event.getErrorCode(), now);
    }


    /** S2: an open ticket (manual or automatic) for the same customer and error code, opened in the last 24 h. */
    public Optional<Ticket> findOpenDuplicate(DetectedErrorEvent event, Instant now)
    {
        return ticketRepository.findFirstByCustomerIdAndErrorCodeAndStatusNotInAndCreatedAtAfterOrderByCreatedAtDesc(
                event.getCustomer().getId(), event.getErrorCode(), CLOSED, now.minus(props.duplicateWindow()));
    }

    public void joinTimeline(Ticket ticket, DetectedErrorEvent event)
    {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("errorEventId", event.getId());
        after.put("errorCode", event.getErrorCode());
        after.put("platform", event.getPlatform().name());
        after.put("occurredAt", event.getOccurredAt().toString());
        // No logged-in user in a scheduled job: a null user records it as a system action.
        auditService.record(AuditAction.ERROR_EVENT_JOINED, null, TicketAuditListener.ENTITY,
                String.valueOf(ticket.getTicketNumber()), null, after);
    }
}
