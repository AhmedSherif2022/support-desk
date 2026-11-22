package com.supportdesk.ticket.repository;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;



public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {


    @EntityGraph(attributePaths = {"customer", "category", "category.parent", "assignedAgent"})
    Optional<Ticket> findByTicketNumber(Long ticketNumber);


    Optional<Ticket> findFirstByCustomerIdAndCategoryIdAndStatusNotIn(Long customerId,
                                                                      Long categoryId,
                                                                      Collection<TicketStatus> closedStatuses);


    long countByCustomerIdAndStatusNotIn(Long customerId, Collection<TicketStatus> closedStatuses);

    @EntityGraph(attributePaths = {"category", "category.parent"})
    List<Ticket> findByCustomerIdOrderByCreatedAtDesc(Long customerId);


    @EntityGraph(attributePaths = {"customer", "category", "category.parent", "assignedAgent"})
    List<Ticket> findByStatusNotIn(Collection<TicketStatus> closedStatuses);

    List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status);


    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    Long nextTicketNumber();


    // E6-S2: open ticket (any origin) for the same customer + error code, opened since the duplicate window began.
    Optional<Ticket> findFirstByCustomerIdAndErrorCodeAndStatusNotInAndCreatedAtAfterOrderByCreatedAtDesc(
            Long customerId, String errorCode, Collection<TicketStatus> closedStatuses, Instant since);
}