package com.supportdesk.azure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportdesk.azure.client.AzureDevOpsClient.WorkItemRequest;
import com.supportdesk.azure.entity.AzureOutboxMessage;
import com.supportdesk.azure.enums.OutboxAggregate;
import com.supportdesk.azure.enums.OutboxOperation;
import com.supportdesk.azure.repository.AzureOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * E5-S1/S3: queues calls to Azure DevOps. Must run inside the caller's transaction, so the outbox row commits (or
 * rolls back) together with the ticket change. The worker sends it later; Azure being down never blocks the agent.
 */
@Service
@RequiredArgsConstructor
public class AzureOutboxService
{
    private final AzureOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Queues a work item. The key (TICKET-2001, INCIDENT-7) is unique: queuing it again returns the existing message,
     * so one ticket or incident never gets two work items.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AzureOutboxMessage queueWorkItem(OutboxAggregate aggregate, Long aggregateId, WorkItemRequest request)
    {
        return outboxRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseGet(() -> outboxRepository.save(new AzureOutboxMessage(aggregate, aggregateId,
                        request.idempotencyKey(), OutboxOperation.CREATE_WORK_ITEM, json(request), clock.instant())));
    }

    /**
     * Queues a comment on the aggregate's work item (Member 3: "Ask SRE for update", "Still not working").
     * Sent once the work item exists.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AzureOutboxMessage queueComment(OutboxAggregate aggregate, Long aggregateId, String text)
    {
        String key = aggregate + "-" + aggregateId + "-COMMENT-" + UUID.randomUUID();
        return outboxRepository.save(new AzureOutboxMessage(aggregate, aggregateId, key,
                OutboxOperation.ADD_COMMENT, json(new CommentPayload(text)), clock.instant()));
    }

    /** The JSON stored for an ADD_COMMENT message. */
    public record CommentPayload(String text)
    {
    }

    private String json(Object value)
    {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write the outbox payload", e);
        }
    }
}
