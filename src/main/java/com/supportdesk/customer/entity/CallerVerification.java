package com.supportdesk.customer.entity;
import com.supportdesk.auth.entity.User;
import com.supportdesk.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "caller_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallerVerification extends BaseEntity
{
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "caller_verification_checks",
            joinColumns = @JoinColumn(name = "verification_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "check_name", nullable = false, length = 40)
    private Set<VerificationCheck> passedChecks = new HashSet<>();

    @Column(nullable = false)
    private Instant verifiedAt;


    @Column(nullable = false)
    private Instant expiresAt;


    private Instant endedAt;

    public CallerVerification(Customer customer, User agent, Set<VerificationCheck> passedChecks,
                              Instant verifiedAt, Instant expiresAt)
    {
        this.customer = customer;
        this.agent = agent;
        this.passedChecks = new HashSet<>(passedChecks);
        this.verifiedAt = verifiedAt;
        this.expiresAt = expiresAt;
    }


    public boolean isActive(Instant now)
    {
        return endedAt == null && expiresAt.isAfter(now);
    }


    public void end(Instant when)
    {
        if (this.endedAt == null) {
            this.endedAt = when;
        }
    }
}
