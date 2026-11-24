package com.supportdesk.health.repository;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.health.service.ServiceHealthSignals;
import com.supportdesk.incident.enums.IncidentStatus;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Repository
@RequiredArgsConstructor
public class ServiceHealthQuery implements ServiceHealthSignals
{
    private static final List<TicketStatus> CLOSED = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final EntityManager em;


    @Override
    public List<IncidentHit> openIncidentsWithOpenTickets()
    {
        List<Object[]> rows = em.createQuery("""
                select i.platform, i.service, i.id
                from Incident i
                where i.status = :open
                  and exists (
                    select 1 from IncidentTicket link
                    where link.incident = i and link.ticket.status not in :closed)
                """, Object[].class)
                .setParameter("open", IncidentStatus.OPEN)
                .setParameter("closed", CLOSED)
                .getResultList();
        return rows.stream()
                .map(row -> new IncidentHit((Platform) row[0], upper(row[1]), (Long) row[2]))
                .toList();
    }

    @Override
    public List<CustomerCount> distinctCustomersSince(Instant from)
    {
        List<Object[]> rows = em.createQuery("""
                select e.platform, upper(e.service), count(distinct e.customer.id)
                from DetectedErrorEvent e
                where e.occurredAt >= :from
                group by e.platform, upper(e.service)
                """, Object[].class)
                .setParameter("from", from)
                .getResultList();
        return rows.stream()
                .map(row -> new CustomerCount((Platform) row[0], (String) row[1], ((Number) row[2]).intValue()))
                .toList();
    }

    @Override
    public List<NoticeHit> openAutoTicketNotices()
    {
        List<Object[]> rows = em.createQuery("""
                select e.platform, upper(e.service), t.ticketNumber, t.priority, t.slaState
                from DetectedErrorEvent e
                join e.ticket t
                where t.origin = :origin
                  and t.status not in :closed
                  and (t.priority = :critical or t.slaState = :breached)
                """, Object[].class)
                .setParameter("origin", TicketOrigin.AUTO_CREATED)
                .setParameter("closed", CLOSED)
                .setParameter("critical", TicketPriority.CRITICAL)
                .setParameter("breached", SlaState.BREACHED)
                .getResultList();
        return rows.stream()
                .map(row -> new NoticeHit((Platform) row[0], (String) row[1], (Long) row[2],
                        row[3] == TicketPriority.CRITICAL, row[4] == SlaState.BREACHED))
                .toList();
    }

    private static String upper(Object service)
    {
        return service == null ? "" : service.toString().toUpperCase(Locale.ROOT);
    }
}
