package com.supportdesk.customer.repository;

import com.supportdesk.customer.enums.ActivityResult;
import com.supportdesk.customer.entity.CustomerActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CustomerActivityRepository extends JpaRepository<CustomerActivity, Long>
{
    /** Newest first, within [from, to). Uses idx_activity_customer_time. */
    List<CustomerActivity> findByCustomerIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            Long customerId, Instant from, Instant to);

    /** The customer's latest failure on one service: its error code goes onto a new ticket (E3-S1). */
    Optional<CustomerActivity> findFirstByCustomerIdAndServiceAndResultOrderByOccurredAtDesc(
            Long customerId, String service, ActivityResult result);
}
