package com.supportdesk.monitor.event;

import com.supportdesk.customer.enums.Platform;

import java.time.Instant;


public record AutoTicketCreatedEvent(
        Long ticketId,
        Long errorEventId,
        String errorCode,
        Platform platform,
        boolean autoEscalate,
        Instant occurredAt)
{
}
