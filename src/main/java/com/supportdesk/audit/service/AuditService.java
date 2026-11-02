package com.supportdesk.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.repository.AuditLogRepository;
import com.supportdesk.auth.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;

/**
 * Writes business audit records (E1-S3). Callers pass what happened; the request's URL, method and IP are added here.
 */
@Service
@RequiredArgsConstructor
public class AuditService
{
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    // MANDATORY: the row joins the caller's business transaction, so the action and its audit commit or roll back together.
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditAction action, AuthenticatedUser user, String entity, String entityId,
                       Object before, Object after)
    {
        HttpServletRequest request = currentRequest();
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .userId(user == null ? null : user.id())
                .username(user == null ? null : user.username())
                .role(user == null ? null : user.role())
                .entity(entity)
                .entityId(entityId)
                .beforeState(toJson(before))
                .afterState(toJson(after))
                .endpoint(request == null ? null : request.getRequestURI())
                .httpMethod(request == null ? null : request.getMethod())
                .ipAddress(request == null ? null : request.getRemoteAddr())   // never X-Forwarded-For, see SecurityErrorWriter
                .timestamp(clock.instant())
                .build());
    }

    /** Same as {@link #record}, for the user logged in on this request. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordForCurrentUser(AuditAction action, String entity, String entityId, Object before, Object after)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedUser user = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser badge
                ? badge : null;
        record(action, user, entity, entityId, before, after);
    }

    private String toJson(Object value)
    {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // Losing the before/after silently would break the audit trail, so fail the business action instead.
            throw new IllegalStateException("Could not write audit state as JSON", e);
        }
    }

    private static HttpServletRequest currentRequest()
    {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
    }
}
