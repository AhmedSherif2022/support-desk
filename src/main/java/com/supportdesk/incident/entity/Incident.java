package com.supportdesk.incident.entity;
import com.supportdesk.common.entity.BaseEntity;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.incident.enums.IncidentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * E6-S3: one grouped outage — the same error code on the same platform, hit by many different customers within the
 * grouping window. Tickets join it through {@link IncidentTicket}. E5 is postponed: no Azure work item here yet.
 */
@Entity
@Table(name = "incident")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident extends BaseEntity
{
    @Column(name = "error_code", nullable = false, length = 40)
    private String errorCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(nullable = false, length = 40)
    private String service;                           // e.g. PAYMENTS, from the detected errors

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "affected_customer_count", nullable = false)
    private int affectedCustomerCount;

    @Column(name = "first_occurred_at", nullable = false)
    private Instant firstOccurredAt;                  // earliest linked error

    @Column(name = "last_occurred_at", nullable = false)
    private Instant lastOccurredAt;                   // latest linked error

    @Column(name = "resolved_at")
    private Instant resolvedAt;


    public Incident(String errorCode, Platform platform, String service, Instant firstOccurredAt, Instant lastOccurredAt)
    {
        this.errorCode = errorCode;
        this.platform = platform;
        this.service = service;
        this.firstOccurredAt = firstOccurredAt;
        this.lastOccurredAt = lastOccurredAt;
    }


    /** Called after tickets were linked: widens the error window and refreshes the distinct customer count. */
    public void recordLinkedErrors(Instant earliest, Instant latest, int affectedCustomerCount)
    {
        if (earliest.isBefore(firstOccurredAt)) {
            firstOccurredAt = earliest;
        }
        if (latest.isAfter(lastOccurredAt)) {
            lastOccurredAt = latest;
        }
        this.affectedCustomerCount = affectedCustomerCount;
    }

    public void markSreFixed()
    {
        this.status = IncidentStatus.SRE_FIXED;
    }

    public void resolve(Instant now)
    {
        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = now;
    }

    public boolean isOpen()
    {
        return status == IncidentStatus.OPEN;
    }
}
