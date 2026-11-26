package com.supportdesk.azure.repository;

import com.supportdesk.azure.entity.AzureOutboxMessage;
import com.supportdesk.azure.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AzureOutboxRepository extends JpaRepository<AzureOutboxMessage, Long>
{
    /** The next due message, locked; SKIP LOCKED lets several app instances share the queue safely. */
    @Query(value = """
            SELECT * FROM azure_outbox
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<AzureOutboxMessage> lockNextDue(@Param("now") Instant now);

    Optional<AzureOutboxMessage> findByIdempotencyKey(String idempotencyKey);

    List<AzureOutboxMessage> findByAggregateIdAndStatus(Long aggregateId, OutboxStatus status);

    long countByStatus(OutboxStatus status);
}
