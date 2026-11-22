package com.supportdesk.incident.repository;
import com.supportdesk.incident.entity.IncidentTicket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IncidentTicketRepository extends JpaRepository<IncidentTicket, Long>
{
    @EntityGraph(attributePaths = {"ticket", "ticket.customer", "ticket.category"})
    List<IncidentTicket> findByIncidentIdOrderByLinkedAtAscIdAsc(Long incidentId);

    long countByIncidentId(Long incidentId);

    /** Ticket counts for a page of incidents in one query: [incidentId, count] rows. */
    @Query("SELECT it.incident.id, COUNT(it) FROM IncidentTicket it WHERE it.incident.id IN :incidentIds GROUP BY it.incident.id")
    List<Object[]> countByIncidentIds(@Param("incidentIds") Collection<Long> incidentIds);

    /** Different customers behind the incident's tickets, not the number of tickets. */
    @Query("SELECT COUNT(DISTINCT it.ticket.customer.id) FROM IncidentTicket it WHERE it.incident.id = :incidentId")
    int countDistinctCustomers(@Param("incidentId") Long incidentId);
}
