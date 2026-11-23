package com.supportdesk.incident.entity;
import com.supportdesk.ticket.entity.Ticket;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One ticket inside one incident. The database keeps ticket_id unique, so a ticket can never be in two incidents. */
@Entity
@Table(name = "incident_ticket")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentTicket
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;


    public IncidentTicket(Incident incident, Ticket ticket, Instant linkedAt)
    {
        this.incident = incident;
        this.ticket = ticket;
        this.linkedAt = linkedAt;
    }
}
