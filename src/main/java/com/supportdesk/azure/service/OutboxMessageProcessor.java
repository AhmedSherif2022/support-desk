package com.supportdesk.azure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.azure.client.AzureDevOpsClient;
import com.supportdesk.azure.client.AzureDevOpsClient.WorkItem;
import com.supportdesk.azure.client.AzureDevOpsClient.WorkItemRequest;
import com.supportdesk.azure.config.AzureDevOpsProperties;
import com.supportdesk.azure.entity.AzureOutboxMessage;
import com.supportdesk.azure.entity.Escalation;
import com.supportdesk.azure.enums.OutboxAggregate;
import com.supportdesk.azure.event.AzureWorkItemCreatedEvent;
import com.supportdesk.azure.repository.AzureOutboxRepository;
import com.supportdesk.azure.repository.EscalationRepository;
import com.supportdesk.azure.service.AzureOutboxService.CommentPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * E5-S3: sends one due outbox message per transaction. The row is locked with SKIP LOCKED, so two app instances never
 * send the same message. On failure it waits with exponential backoff and keeps the same idempotency key; after the
 * maximum attempts it is FAILED, the escalation shows Failed, AZURE_SYNC_FAILED is audited and the alert is logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxMessageProcessor
{
    private final AzureOutboxRepository outboxRepository;
    private final EscalationRepository escalationRepository;
    private final AzureDevOpsClient client;
    private final AzureDevOpsProperties props;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;
    private final Clock clock;

    /** Sends the next due message. False when nothing is due. */
    public boolean processNext()
    {
        return Boolean.TRUE.equals(transactions.execute(status -> {
            Optional<AzureOutboxMessage> next = outboxRepository.lockNextDue(clock.instant());
            next.ifPresent(this::send);
            return next.isPresent();
        }));
    }

    private void send(AzureOutboxMessage message)
    {
        Instant now = clock.instant();
        try {
            switch (message.getOperation()) {
                case CREATE_WORK_ITEM -> createWorkItem(message, now);
                case ADD_COMMENT -> addComment(message, now);
            }
        } catch (RuntimeException e) {
            fail(message, e, now);
        }
    }

    private void createWorkItem(AzureOutboxMessage message, Instant now) throws RuntimeException
    {
        WorkItemRequest request = read(message.getPayload(), WorkItemRequest.class);
        WorkItem workItem = client.createWorkItem(request);
        message.markSent(workItem.id(), now);
        if (message.getAggregateType() == OutboxAggregate.TICKET) {
            escalationRepository.findByTicketId(message.getAggregateId())
                    .ifPresent(escalation -> escalation.markSynced(workItem, now));
        }
        events.publishEvent(new AzureWorkItemCreatedEvent(message.getAggregateType(), message.getAggregateId(),
                message.getIdempotencyKey(), workItem.id(), workItem.state(), workItem.revision()));
        log.info("Azure work item {} created for {}", workItem.id(), message.getIdempotencyKey());
    }

    private void addComment(AzureOutboxMessage message, Instant now)
    {
        Long workItemId = workItemFor(message);
        if (workItemId == null) {
            message.postpone(now.plus(props.outbox().firstRetryDelay()));   // the work item is not created yet
            return;
        }
        client.addComment(workItemId, read(message.getPayload(), CommentPayload.class).text());
        message.markSent(workItemId, now);
    }

    /** The work item a comment goes to: the ticket's escalation, or (Member 2) the incident's work item message. */
    private Long workItemFor(AzureOutboxMessage message)
    {
        if (message.getAggregateType() == OutboxAggregate.TICKET) {
            return escalationRepository.findByTicketId(message.getAggregateId())
                    .map(Escalation::getWorkItemId)
                    .orElse(null);
        }
        return outboxRepository.findByIdempotencyKey("INCIDENT-" + message.getAggregateId())
                .map(AzureOutboxMessage::getWorkItemId)
                .orElse(null);
    }

    private void fail(AzureOutboxMessage message, RuntimeException error, Instant now)
    {
        int maxAttempts = props.outbox().maxAttempts();
        Duration delay = backoff(message.getAttempts() + 1, props.outbox().firstRetryDelay(),
                props.outbox().maxRetryDelay());
        message.recordFailure(error.getMessage(), now.plus(delay), maxAttempts);

        if (!message.isFailed()) {
            log.warn("Azure DevOps call {} failed (attempt {}/{}), retrying in {}: {}", message.getIdempotencyKey(),
                    message.getAttempts(), maxAttempts, delay, error.getMessage());
            return;
        }
        if (message.getAggregateType() == OutboxAggregate.TICKET) {
            escalationRepository.findByTicketId(message.getAggregateId()).ifPresent(Escalation::markFailed);
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("operation", message.getOperation().name());
        after.put("attempts", message.getAttempts());
        after.put("lastError", message.getLastError());
        auditService.record(AuditAction.AZURE_SYNC_FAILED, null, "AZURE_OUTBOX", message.getIdempotencyKey(), null, after);
        // The failed-integration alert: an ERROR log line for log-based alerting, plus GET /api/admin/azure/outbox.
        log.error("ALERT failed integration: Azure DevOps call {} gave up after {} attempts: {}",
                message.getIdempotencyKey(), message.getAttempts(), message.getLastError());
    }

    /** Exponential backoff: first, 2x first, 4x first ... capped at max. {@code failures} counts from 1. */
    static Duration backoff(int failures, Duration first, Duration max)
    {
        if (failures <= 1) {
            return first;
        }
        long factor = 1L << Math.min(failures - 1, 30);
        Duration delay = first.multipliedBy(factor);
        return delay.compareTo(max) > 0 ? max : delay;
    }

    private <T> T read(String json, Class<T> type)
    {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Unreadable outbox payload: " + e.getMessage(), e);
        }
    }
}
