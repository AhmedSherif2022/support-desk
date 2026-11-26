package com.supportdesk.azure.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.azure.dto.EscalationDtos.EscalateRequest;
import com.supportdesk.azure.dto.EscalationDtos.EscalationDto;
import com.supportdesk.azure.dto.EscalationDtos.FailedMessageDto;
import com.supportdesk.azure.dto.EscalationDtos.OutboxSummaryDto;
import com.supportdesk.azure.entity.AzureOutboxMessage;
import com.supportdesk.azure.entity.Escalation;
import com.supportdesk.azure.enums.OutboxAggregate;
import com.supportdesk.azure.enums.OutboxStatus;
import com.supportdesk.azure.enums.SyncStatus;
import com.supportdesk.azure.repository.AzureOutboxRepository;
import com.supportdesk.azure.repository.EscalationRepository;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.StepActor;
import com.supportdesk.ticket.enums.TicketStep;
import com.supportdesk.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * E5-S1: an agent escalates a ticket to SRE. In one transaction: the ticket moves to Escalated (step HAND_OVER),
 * the escalation is saved, and the work item is queued in the outbox. The worker sends it to Azure DevOps later
 * (E5-S3), so the agent is never blocked by Azure.
 */
@Service
@RequiredArgsConstructor
public class EscalationService
{
    static final String QUEUED = "Sent to SRE. Azure DevOps will confirm the work item shortly; no action is needed.";
    static final String RETRYING = "Azure DevOps is not reachable right now. We keep retrying automatically; "
            + "no action is needed.";
    static final String FAILED = "Could not reach Azure DevOps after several attempts. The integration team has "
            + "been alerted; no action is needed from you.";

    private final TicketService ticketService;
    private final EscalationRepository escalationRepository;
    private final AzureOutboxRepository outboxRepository;
    private final AzureOutboxService outboxService;
    private final EscalationPayloadBuilder payloadBuilder;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public EscalationDto escalate(Long ticketNumber, EscalateRequest request)
    {
        Ticket ticket = ticketService.getEntity(ticketNumber);
        if (escalationRepository.existsByTicketId(ticket.getId())) {
            throw new BusinessRuleException(ErrorCode.ALREADY_ESCALATED);
        }
        User agent = currentUserService.getCurrentUserEntity();
        String reason = request.reason().trim();

        // Troubleshooting -> Escalated; anything else is refused with 422 INVALID_STATUS_TRANSITION (and audited).
        ticketService.applyStep(ticket, TicketStep.HAND_OVER, StepActor.SYSTEM, agent,
                "Escalated to SRE (" + request.urgency() + "): " + reason);

        Instant now = clock.instant();
        Escalation escalation = escalationRepository.save(new Escalation(ticket, reason, request.urgency(), agent, now));

        // TODO (E5-S2, Member 2): an open incident on the same service + error code -> link to its work item instead.
        String key = workItemKey(ticket);
        outboxService.queueWorkItem(OutboxAggregate.TICKET, ticket.getId(),
                payloadBuilder.build(ticket, escalation, key).toWorkItemRequest());

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("urgency", request.urgency().name());
        after.put("workItemKey", key);
        auditService.recordForCurrentUser(AuditAction.TICKET_ESCALATED, "TICKET",
                String.valueOf(ticket.getTicketNumber()), null, after);

        return toDto(escalation, QUEUED);
    }

    @Transactional(readOnly = true)
    public EscalationDto get(Long ticketNumber)
    {
        Ticket ticket = ticketService.getEntity(ticketNumber);
        Escalation escalation = escalationRepository.findByTicketId(ticket.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.ESCALATION_NOT_FOUND));
        return toDto(escalation, messageFor(escalation, workItemKey(ticket)));
    }

    /** Guide Admin: the failed-integration monitor (E5-S3). */
    @Transactional(readOnly = true)
    public OutboxSummaryDto outboxSummary()
    {
        return new OutboxSummaryDto(
                outboxRepository.countByStatus(OutboxStatus.PENDING),
                outboxRepository.countByStatus(OutboxStatus.FAILED),
                outboxRepository.findAll().stream()
                        .filter(AzureOutboxMessage::isFailed)
                        .map(m -> new FailedMessageDto(m.getIdempotencyKey(), m.getOperation().name(), m.getAttempts(),
                                m.getLastError(), m.getUpdatedAt()))
                        .toList());
    }

    /** One work item per ticket: the outbox key, also stored on the work item as a tag. */
    static String workItemKey(Ticket ticket)
    {
        return "TICKET-" + ticket.getTicketNumber();
    }

    private String messageFor(Escalation escalation, String key)
    {
        return switch (escalation.getSyncStatus()) {
            case SYNCED -> "SRE work item " + escalation.getWorkItemId() + " (" + escalation.getAzureState() + ").";
            case FAILED -> FAILED;
            case PENDING -> outboxRepository.findByIdempotencyKey(key)
                    .filter(m -> m.getAttempts() > 0)
                    .map(m -> RETRYING)
                    .orElse(QUEUED);
        };
    }

    private static EscalationDto toDto(Escalation e, String message)
    {
        Ticket ticket = e.getTicket();
        SyncStatus sync = e.getSyncStatus();
        return new EscalationDto(ticket.displayNumber(), ticket.getStatus(), e.getUrgency(), e.getReason(),
                e.getEscalatedBy() == null ? null : e.getEscalatedBy().getFullName(), e.getEscalatedAt(),
                sync, sync.getLabel(), e.getWorkItemId(), e.getAzureState(), message);
    }
}
