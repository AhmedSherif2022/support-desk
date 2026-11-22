package com.supportdesk.monitor.repository;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

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
}
