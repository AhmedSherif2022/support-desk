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


    // Admin screens: every category (active or not), top-level ones and the sub-categories under one parent.
    List<TicketCategory> findByParentIsNullOrderByNameAsc();

    List<TicketCategory> findByParentIdOrderByNameAsc(Long parentId);

    @EntityGraph(attributePaths = "parent")
    List<TicketCategory> findByParentIsNotNullOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
