package com.supportdesk.audit.controller;

import com.supportdesk.audit.dto.AuditLogResponse;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditQueryService;
import com.supportdesk.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;


@RestController
@PreAuthorize("hasAnyRole('GUIDE_ADMIN', 'REVIEWER')")
@RequiredArgsConstructor
public class AuditController implements AuditApi
{
    private final AuditQueryService auditQueryService;

    @Override
    public PageResponse<AuditLogResponse> search(String username, AuditAction action, String entity, String entityId,
                                                 Instant from, Instant to, Pageable pageable)
    {
        return PageResponse.of(auditQueryService.search(username, action, entity, entityId, from, to, pageable)
                .map(AuditLogResponse::from));
    }
}
