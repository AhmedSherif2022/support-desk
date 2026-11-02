package com.supportdesk.ticket.entity;

import com.supportdesk.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** What the agent told the customer (E3-S6). A record of the past, so it is never edited. */
@Entity
@Immutable
@Table(name = "ticket_contacts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketContact
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 2000)
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    public TicketContact(Ticket ticket, User author, String note, Instant createdAt)
    {
        this.ticket = ticket;
        this.author = author;
        this.note = note;
        this.createdAt = createdAt;
    }
}
