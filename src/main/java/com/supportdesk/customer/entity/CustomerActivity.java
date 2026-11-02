package com.supportdesk.customer.entity;
import com.supportdesk.customer.enums.ActivityResult;
import com.supportdesk.customer.enums.Platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** One thing the customer did on a channel (login, transfer...). Written by the channels; this app only reads it. */
@Entity
@Immutable
@Table(name = "customer_activity_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerActivity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    /** Top-level ticket category code, e.g. AUTHENTICATION. */
    @Column(nullable = false, length = 50)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivityResult result;

    @Column(length = 40)
    private String errorCode;

    @Column(nullable = false)
    private Instant createdAt;

    public boolean isFailure()
    {
        return result == ActivityResult.FAILURE;
    }
}
