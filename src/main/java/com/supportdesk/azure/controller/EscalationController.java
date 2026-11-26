package com.supportdesk.azure.controller;

import com.supportdesk.azure.dto.EscalationDtos.EscalateRequest;
import com.supportdesk.azure.dto.EscalationDtos.EscalationDto;
import com.supportdesk.azure.service.EscalationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class EscalationController implements EscalationApi
{
    private final EscalationService escalationService;

    @Override
    public EscalationDto escalate(Long number, EscalateRequest request)
    {
        return escalationService.escalate(number, request);
    }

    @Override
    public EscalationDto get(Long number)
    {
        return escalationService.get(number);
    }
}
