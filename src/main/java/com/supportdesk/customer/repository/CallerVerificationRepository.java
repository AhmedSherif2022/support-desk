package com.supportdesk.customer.repository;

import com.supportdesk.customer.entity.CallerVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;


public interface CallerVerificationRepository extends JpaRepository<CallerVerification, Long>
{

    Optional<CallerVerification> findFirstByAgentIdAndCustomerIdAndEndedAtIsNullAndExpiresAtAfterOrderByVerifiedAtDesc(
            Long agentId, Long customerId, Instant now);
}
