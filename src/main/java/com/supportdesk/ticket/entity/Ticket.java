package com.supportdesk.ticket.entity;
import com.supportdesk.auth.entity.User;
import com.supportdesk.common.entity.BaseEntity;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.entity.Platform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Duration;
import java.time.Instant;


@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseEntity {


    @Column(nullable = false, unique = true)
    private Long ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private TicketCategory category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TicketStatus status = TicketStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TicketOrigin origin;


    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Platform platform;


    @Column(length = 40)
    private String errorCode;


    @Column(length = 40)
    private String reference;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;



    @Column(nullable = false)
    private Instant slaStartedAt;

    @Column(nullable = false)
    private Instant slaDueAt;

    private Instant slaPausedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SlaState slaState = SlaState.ON_TRACK;

    private Instant resolvedAt;

    private Instant closedAt;

    @Column(nullable = false)
    private int reopenCount;

    private Instant lastCustomerContactAt;


    public Ticket(Long ticketNumber, Customer customer, TicketCategory category, String title,
                  String description, TicketPriority priority, TicketOrigin origin, Platform platform,
                  String errorCode, String reference, User createdBy, Instant slaStartedAt, Instant slaDueAt) {
        this.ticketNumber = ticketNumber;
        this.customer = customer;
        this.category = category;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.origin = origin;
        this.platform = platform;
        this.errorCode = errorCode;
        this.reference = reference;
        this.createdBy = createdBy;
        this.slaStartedAt = slaStartedAt;
        this.slaDueAt = slaDueAt;
    }


    public String displayNumber() {
        return "#" + ticketNumber;
    }


    public void moveTo(TicketStatus newStatus, Instant now) {
        TicketStatus old = this.status;


        if (old == TicketStatus.WAITING_ON_CUSTOMER && slaPausedAt != null) {
            slaDueAt = slaDueAt.plus(Duration.between(slaPausedAt, now));
            slaPausedAt = null;
        }
        // Entering "waiting on customer": stop the clock.
        if (newStatus == TicketStatus.WAITING_ON_CUSTOMER) {
            slaPausedAt = now;
        }
        if (newStatus == TicketStatus.RESOLVED) {
            resolvedAt = now;
        }
        if (newStatus == TicketStatus.CLOSED) {
            closedAt = now;
        }
        if (newStatus == TicketStatus.REOPENED) {
            reopenCount++;
            resolvedAt = null;
        }
        this.status = newStatus;
    }


    public boolean updateSlaState(SlaState newState)
    {
        if (this.slaState == newState) {
            return false;
        }
        this.slaState = newState;
        return true;
    }

    public void assignTo(User agent)
    {
        this.assignedAgent = agent;
    }

    public void recordCustomerContact(Instant when)
    {
        this.lastCustomerContactAt = when;
    }

    public boolean isOpen()
    {
        return status.isOpen();
    }

    public boolean isSlaPaused()
    {
        return slaPausedAt != null;
    }
}
