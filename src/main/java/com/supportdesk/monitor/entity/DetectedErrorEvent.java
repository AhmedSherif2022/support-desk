package com.supportdesk.monitor.entity;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.ticket.entity.Ticket;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
@Entity
@Table(name = "detected_error_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetectedErrorEvent
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "error_code", nullable = false, length = 40)
    private String errorCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;                        // MOBILE / WEB

    @Column(nullable = false, length = 40)
    private String service;                           // e.g. PAYMENTS

    @Column(length = 500)
    private String message;

    @Column(name = "correlation_id", length = 80)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "processed_at")
    private Instant processedAt;                      // null = not processed yet

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EventOutcome outcome;                     // what the job did with it

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;                            // the ticket it created or joined; null for UNKNOWN_CODE


    public DetectedErrorEvent(Customer customer, String errorCode, Platform platform, String service,
                              String message, String correlationId, Instant occurredAt)
    {
        this.customer = customer;
        this.errorCode = errorCode.trim().toUpperCase();
        this.platform = platform;
        this.service = service;
        this.message = message;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }


    public void markProcessed(EventOutcome outcome, Ticket ticket, Instant now)
    {
        if (processedAt != null) {
            throw new IllegalStateException("Error event " + id + " was already processed");
        }
        this.outcome = outcome;
        this.ticket = ticket;
        this.processedAt = now;
    }

    public void markGrouped()
    {
        this.outcome = EventOutcome.GROUPED;
    }

    public boolean isProcessed()
    {
        return processedAt != null;
    }
}