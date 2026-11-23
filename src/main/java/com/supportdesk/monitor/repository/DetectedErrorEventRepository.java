package com.supportdesk.monitor.repository;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.ticket.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DetectedErrorEventRepository extends JpaRepository<DetectedErrorEvent, Long>
{

    @Query(value = """
            SELECT * FROM detected_error_event
            WHERE processed_at IS NULL
            ORDER BY occurred_at, id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<DetectedErrorEvent> lockUnprocessed(@Param("limit") int limit);


    /** Sets aside a row whose processing threw; no-op if another run already handled it. */
    @Modifying
    @Query(value = """
            UPDATE detected_error_event
            SET processed_at = :now, outcome = 'FAILED'
            WHERE id = :id AND processed_at IS NULL
            """, nativeQuery = true)
    int markFailed(@Param("id") Long id, @Param("now") Instant now);


    long countByProcessedAtIsNull();


    List<DetectedErrorEvent> findByTicketIdOrderByOccurredAtDesc(Long ticketId);


    /**
     * E6-S3: errors that may still form or join an incident — rows (CREATED or DUPLICATE) handled since {@code since}
     * or that occurred since {@code occurredFrom}, whose automatic ticket is still open and not linked to any incident
     * yet. Ordered so one pass can walk them group by group. Both bounds keep older rows from being read on every run.
     */
    @Query("""
            SELECT e FROM DetectedErrorEvent e
            JOIN FETCH e.ticket t
            WHERE e.outcome IN (com.supportdesk.monitor.enums.EventOutcome.CREATED,
                                com.supportdesk.monitor.enums.EventOutcome.DUPLICATE)
              AND t.origin = com.supportdesk.ticket.enums.TicketOrigin.AUTO_CREATED
              AND t.status NOT IN :closedStatuses
              AND NOT EXISTS (SELECT 1 FROM IncidentTicket it WHERE it.ticket = t)
              AND (e.processedAt >= :since OR e.occurredAt >= :occurredFrom)
            ORDER BY e.errorCode, e.platform, e.occurredAt, e.id
            """)
    List<DetectedErrorEvent> findGroupingCandidates(@Param("closedStatuses") Collection<TicketStatus> closedStatuses,
                                                    @Param("since") Instant since,
                                                    @Param("occurredFrom") Instant occurredFrom);

    /** When the earliest error handled since {@code since} occurred; empty when the monitor handled none. */
    @Query("SELECT MIN(e.occurredAt) FROM DetectedErrorEvent e WHERE e.processedAt >= :since")
    Optional<Instant> findEarliestOccurredHandledSince(@Param("since") Instant since);
}
