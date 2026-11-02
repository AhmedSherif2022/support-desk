package com.supportdesk.audit.event;

import com.supportdesk.auth.enums.Role;

import java.time.Instant;

public record AccessDeniedEvent(
        Long userId,
        String username,
        Role role,
        String endpoint,
        String httpMethod,
        String ipAddress,
        Instant timestamp
) {
    public AccessDeniedEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
