package com.supportdesk.azure.entity;

import com.supportdesk.azure.enums.OutboxAggregate;
import com.supportdesk.azure.enums.OutboxOperation;
import com.supportdesk.azure.enums.OutboxStatus;
import com.supportdesk.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * E5-S3: one call to Azure DevOps, written in the same transaction as the ticket change and sent later by the
 * outbox worker. The idempotency key is unique, so the same work item or comment is never queued twice.
 */
@Entity
@Table(name = "azure_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AzureOutboxMessage extends BaseEntity
{
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OutboxAggregate aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxOperation operation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;                              // JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(length = 1000)
    private String lastError;

    private Long workItemId;                             // the work item created, or commented on

    private Instant sentAt;

    public AzureOutboxMessage(OutboxAggregate aggregateType, Long aggregateId, String idempotencyKey,
                              OutboxOperation operation, String payload, Instant now)
    {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.idempotencyKey = idempotencyKey;
        this.operation = operation;
        this.payload = payload;
        this.nextAttemptAt = now;                        // due straight away
    }

    public void markSent(Long workItemId, Instant now)
    {
        this.status = OutboxStatus.SENT;
        this.workItemId = workItemId;
        this.sentAt = now;
        this.lastError = null;
    }

    /** A failed try: count it and wait until {@code nextAttemptAt}; FAILED once {@code maxAttempts} is reached. */
    public void recordFailure(String error, Instant nextAttemptAt, int maxAttempts)
    {
        this.attempts++;
        this.lastError = error == null ? null : (error.length() <= 1000 ? error : error.substring(0, 1000));
        if (attempts >= maxAttempts) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.nextAttemptAt = nextAttemptAt;
        }
    }

    /** Not ready yet (e.g. a comment whose work item does not exist yet): try again later without counting it. */
    public void postpone(Instant nextAttemptAt)
    {
        this.nextAttemptAt = nextAttemptAt;
    }

    public boolean isFailed()
    {
        return status == OutboxStatus.FAILED;
    }
}
