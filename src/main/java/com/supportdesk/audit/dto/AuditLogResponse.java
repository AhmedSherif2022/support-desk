package com.supportdesk.audit.dto;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.auth.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** One audit record as the Audit log screen shows it. */
public record AuditLogResponse(
        @Schema(example = "512") Long id,
        @Schema(example = "2026-11-16T10:35:00Z") Instant time,
        @Schema(description = "Who did it; null for the system (for example SLA changes).", example = "1") Long userId,
        @Schema(example = "mona") String username,
        @Schema(example = "AGENT") Role role,
        @Schema(example = "TICKET_STATUS_CHANGED") AuditAction action,
        @Schema(description = "What the record is about: TICKET (entityId is the ticket number), CUSTOMER "
                + "(entityId is the CIF), GUIDE_FEEDBACK (entityId is the flag id) or INCIDENT (entityId is the incident id).",
                example = "TICKET") String entity,
        @Schema(example = "2001") String entityId,
        @Schema(description = "JSON text of the state before the action; null when there was none. Its shape depends "
                + "on the action (see the ticket Audit tab).") String before,
        @Schema(description = "JSON text of the state after the action, or its details; null when there are none. "
                + "Its shape depends on the action (see the ticket Audit tab).")
        String after,
        @Schema(description = "The request that caused it; null for the system.", example = "/api/tickets/2001/steps") String endpoint,
        @Schema(example = "POST") String httpMethod,
        @Schema(example = "10.0.0.15") String ipAddress)
{
    public static AuditLogResponse from(AuditLog log)
    {
        return new AuditLogResponse(log.getId(), log.getTimestamp(), log.getUserId(), log.getUsername(), log.getRole(),
                log.getAction(), log.getEntity(), log.getEntityId(), log.getBeforeState(), log.getAfterState(),
                log.getEndpoint(), log.getHttpMethod(), log.getIpAddress());
    }
}
