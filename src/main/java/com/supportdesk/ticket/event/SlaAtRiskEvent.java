package com.supportdesk.ticket.event;

import java.time.Instant;

/**
 * Published once when an open ticket crosses the warning threshold (audit action: SLA_WARNING).
 */
public record SlaAtRiskEvent(Long ticketId, Long ticketNumber, int percentUsed, Instant at)
{
}
