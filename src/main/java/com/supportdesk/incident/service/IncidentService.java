package com.supportdesk.incident.service;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.incident.dto.IncidentDtos.IncidentDto;
import com.supportdesk.incident.dto.IncidentDtos.IncidentTicketDto;
import com.supportdesk.incident.entity.Incident;
import com.supportdesk.incident.enums.IncidentStatus;
import com.supportdesk.incident.exception.IncidentNotFoundException;
import com.supportdesk.incident.repository.IncidentRepository;
import com.supportdesk.incident.repository.IncidentTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** E6-S3 read side: what the Agent's frontend shows about incidents. Lifecycle changes (SRE fixed, resolved) arrive with E5. */
@Service
@RequiredArgsConstructor
public class IncidentService
{
    private final IncidentRepository incidentRepository;
    private final IncidentTicketRepository incidentTicketRepository;


    /** Incidents newest first; {@code status} null lists every incident. */
    @Transactional(readOnly = true)
    public PageResponse<IncidentDto> list(IncidentStatus status, Pageable pageable)
    {
        Page<Incident> page = status == null
                ? incidentRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                : incidentRepository.findByStatusOrderByCreatedAtDescIdDesc(status, pageable);

        Map<Long, Long> ticketCounts = ticketCounts(page.getContent());
        return PageResponse.of(page.map(incident ->
                IncidentDto.from(incident, ticketCounts.getOrDefault(incident.getId(), 0L))));
    }


    @Transactional(readOnly = true)
    public IncidentDto get(Long id)
    {
        Incident incident = getEntity(id);
        return IncidentDto.from(incident, incidentTicketRepository.countByIncidentId(id));
    }


    @Transactional(readOnly = true)
    public List<IncidentTicketDto> tickets(Long id)
    {
        getEntity(id);
        return incidentTicketRepository.findByIncidentIdOrderByLinkedAtAscIdAsc(id).stream()
                .map(IncidentTicketDto::from)
                .toList();
    }


    private Incident getEntity(Long id)
    {
        return incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }

    private Map<Long, Long> ticketCounts(List<Incident> incidents)
    {
        Map<Long, Long> counts = new HashMap<>();
        if (incidents.isEmpty()) {
            return counts;
        }
        List<Long> ids = incidents.stream().map(Incident::getId).toList();
        for (Object[] row : incidentTicketRepository.countByIncidentIds(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
