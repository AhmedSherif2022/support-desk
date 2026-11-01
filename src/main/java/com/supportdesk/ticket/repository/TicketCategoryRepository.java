package com.supportdesk.ticket.repository;

import com.supportdesk.ticket.entity.TicketCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {


    @EntityGraph(attributePaths = "parent")   // also load the parent category in the same query
    Optional<TicketCategory> findByCodeIgnoreCase(String code);


    @EntityGraph(attributePaths = "parent")
    List<TicketCategory> findByActiveTrueOrderByNameAsc();
}
