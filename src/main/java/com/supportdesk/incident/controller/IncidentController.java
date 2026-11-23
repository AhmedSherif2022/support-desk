package com.supportdesk.incident.controller;
import com.supportdesk.common.dto.PageResponse;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.incident.dto.IncidentDtos.IncidentDto;
import com.supportdesk.incident.dto.IncidentDtos.IncidentTicketDto;
import com.supportdesk.incident.enums.IncidentStatus;
import com.supportdesk.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class IncidentController implements IncidentApi
{
    private final IncidentService incidentService;

    @Override
    public PageResponse<IncidentDto> list(String status, Pageable pageable)
    {
        return incidentService.list(parseStatus(status), pageable);
    }

    @Override
    public IncidentDto get(Long id)
    {
        return incidentService.get(id);
    }

    @Override
    public List<IncidentTicketDto> tickets(Long id)
    {
        return incidentService.tickets(id);
    }

    /** OPEN, SRE_FIXED, RESOLVED, or ALL for no filter; anything else is a 400 REQUEST_ERROR. */
    private static IncidentStatus parseStatus(String status)
    {
        String value = status == null ? "OPEN" : status.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(value)) {
            return null;
        }
        try {
            return IncidentStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(ErrorCode.REQUEST_ERROR, "status must be OPEN, SRE_FIXED, RESOLVED or ALL.");
        }
    }
}
