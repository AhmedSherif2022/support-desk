package com.supportdesk.audit.listener;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.audit.event.AccessDeniedEvent;
import com.supportdesk.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAccessDenied(AccessDeniedEvent event) {
        try {
            String endpoint = truncate(event.endpoint(), 500);
            String httpMethod = truncate(event.httpMethod(), 10);
            String ipAddress = truncate(event.ipAddress(), 45);
            String username = truncate(event.username(), 100);

            AuditLog auditLog = AuditLog.builder()
                    .userId(event.userId())
                    .username(username)
                    .role(event.role())
                    .action(AuditAction.ACCESS_DENIED)
                    .endpoint(endpoint != null ? endpoint : "UNKNOWN")
                    .httpMethod(httpMethod != null ? httpMethod : "UNKNOWN")
                    .ipAddress(ipAddress)
                    .timestamp(event.timestamp() != null ? event.timestamp() : java.time.Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit log recorded: action=ACCESS_DENIED, user={}, endpoint={}", event.username(), event.endpoint());
        } catch (Exception e) {
            log.error("Failed to persist audit log for access denied event: {}", event, e);
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
