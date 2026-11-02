package com.supportdesk.ticket.event;

import java.time.Instant;

/**
 * Published once when an open ticket uses up its SLA target (audit action: SLA_BREACHED).
 */
public record SlaBreachedEvent(Long ticketId, Long ticketNumber, int percentUsed, Instant at)
{
}
