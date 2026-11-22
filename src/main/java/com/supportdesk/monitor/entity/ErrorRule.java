package com.supportdesk.monitor.entity;

import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketPriority;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Table(name = "error_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorRule
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_code", nullable = false, length = 40, unique = true)
    private String errorCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private TicketCategory category;

    @Column(nullable = false, length = 150)
    private String issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TicketPriority priority;

    @Column(name = "auto_escalate", nullable = false)
    private boolean autoEscalate;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ErrorRule(String errorCode, TicketCategory category, String issue, TicketPriority priority,
                     boolean autoEscalate, Instant now)
    {
        this.errorCode = errorCode.trim().toUpperCase();
        this.category = category;
        this.issue = issue.trim();
        this.priority = priority;
        this.autoEscalate = autoEscalate;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }


    public void update(TicketCategory category, String issue, TicketPriority priority, boolean autoEscalate, Instant now)
    {
        this.category = category;
        this.issue = issue.trim();
        this.priority = priority;
        this.autoEscalate = autoEscalate;
        this.updatedAt = now;
    }


    public void deactivate(Instant now)
    {
        this.active = false;
        this.updatedAt = now;
    }
}


