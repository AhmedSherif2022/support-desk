package com.supportdesk.azure.controller;

import com.supportdesk.azure.dto.EscalationDtos.OutboxSummaryDto;
import com.supportdesk.azure.service.EscalationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('GUIDE_ADMIN')")
@RequiredArgsConstructor
public class AzureOutboxAdminController implements AzureOutboxAdminApi
{
    private final EscalationService escalationService;

    @Override
    public OutboxSummaryDto summary()
    {
        return escalationService.outboxSummary();
    }
}
