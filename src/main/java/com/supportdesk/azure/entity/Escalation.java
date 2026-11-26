package com.supportdesk.azure.entity;

import com.supportdesk.auth.entity.User;
import com.supportdesk.azure.client.AzureDevOpsClient.WorkItem;
import com.supportdesk.azure.enums.EscalationUrgency;
import com.supportdesk.azure.enums.SyncStatus;
import com.supportdesk.common.entity.BaseEntity;
import com.supportdesk.ticket.entity.Ticket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * E5-S1: a ticket handed to SRE. One per ticket. Holds the agent's reason and urgency and the Azure DevOps work item
 * once the outbox worker has created it (E5-S3). Members 2 and 3 read and update the Azure fields.
 */
@Entity
@Table(name = "escalation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Escalation extends BaseEntity
{
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", unique = true)
    private Ticket ticket;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EscalationUrgency urgency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_by_id")
    private User escalatedBy;                           // null when the system escalates (E6-S4)

    @Column(nullable = false)
    private Instant escalatedAt;

    private Long workItemId;                            // null until Azure confirms

    @Column(length = 40)
    private String azureState;

    private Integer azureRevision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    private Instant lastSyncedAt;

    public Escalation(Ticket ticket, String reason, EscalationUrgency urgency, User escalatedBy, Instant now)
    {
        this.ticket = ticket;
        this.reason = reason;
        this.urgency = urgency;
        this.escalatedBy = escalatedBy;
        this.escalatedAt = now;
    }

    /** The work item exists in Azure: show its number and state, "synced" (E5-S1). */
    public void markSynced(WorkItem workItem, Instant now)
    {
        this.workItemId = workItem.id();
        this.azureState = workItem.state();
        this.azureRevision = workItem.revision();
        this.syncStatus = SyncStatus.SYNCED;
        this.lastSyncedAt = now;
    }

    /** Gave up after the maximum attempts (E5-S3). */
    public void markFailed()
    {
        this.syncStatus = SyncStatus.FAILED;
    }
}
