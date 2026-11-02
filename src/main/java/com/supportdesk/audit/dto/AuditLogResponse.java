package com.supportdesk.audit.dto;

import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.entity.AuditLog;
import com.supportdesk.auth.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** One audit record as the Audit log screen shows it. */
public record AuditLogResponse(
        Long id,
        Instant time,
        Long userId,
        String username,
        Role role,
        AuditAction action,
        @Schema(example = "CUSTOMER") String entity,
        @Schema(example = "CIF-100245") String entityId,
        @Schema(description = "JSON of the entity before the action, if it changed something.") String before,
        @Schema(description = "JSON of the entity after the action.") String after,
        String endpoint,
        String httpMethod,
        String ipAddress)
{
    public static AuditLogResponse from(AuditLog log)
    {
        return new AuditLogResponse(log.getId(), log.getTimestamp(), log.getUserId(), log.getUsername(), log.getRole(),
                log.getAction(), log.getEntity(), log.getEntityId(), log.getBeforeState(), log.getAfterState(),
                log.getEndpoint(), log.getHttpMethod(), log.getIpAddress());
    }
}
