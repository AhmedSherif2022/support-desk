package com.supportdesk.ticket.repository;

import com.supportdesk.ticket.entity.TicketContact;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketContactRepository extends JpaRepository<TicketContact, Long>
{
    @EntityGraph(attributePaths = "author")
    List<TicketContact> findByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);
}
