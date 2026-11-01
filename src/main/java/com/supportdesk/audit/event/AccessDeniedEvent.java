package com.supportdesk.audit.event;

import java.time.Instant;

public record AccessDeniedEvent(
        Long userId,
        String username,
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
