package com.supportdesk.audit.service;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Read side of the audit trail: filter by user, action, entity and time (E1-S3). Every filter is optional. */
@Service
@RequiredArgsConstructor
public class AuditQueryService
{
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String username, AuditAction action, String entity, String entityId,
                                 Instant from, Instant to, Pageable pageable)
    {
        Specification<AuditLog> spec = Specification.<AuditLog>where(equal("username", username))
                .and(equal("action", action))
                .and(equal("entity", entity))
                .and(equal("entityId", entityId))
                .and(from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from))
                .and(to == null ? null : (root, query, cb) -> cb.lessThan(root.get("createdAt"), to));
        return auditLogRepository.findAll(spec, pageable);
    }

    // A null filter adds no condition.
    private static Specification<AuditLog> equal(String field, Object value)
    {
        return value == null ? null : (root, query, cb) -> cb.equal(root.get(field), value);
    }
}
