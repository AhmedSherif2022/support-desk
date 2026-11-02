package com.supportdesk.ticket.dto;

import com.supportdesk.ticket.enums.SlaState;

import java.time.Duration;

/**
 * One reading of a ticket's SLA clock: how much of the target is used, and which band that is.
 */
public record SlaSnapshot(
        SlaState state,
        int percentUsed,
        Duration elapsed,
        Duration target,
        Duration remaining,
        boolean paused)
{
    /** The one value the ticket header shows (E3-S3): time left, "Paused", "Met", or "Breached Xh Ym". */
    public String label()
    {
        return switch (state) {
            case PAUSED -> "Paused";
            case MET -> "Met";
            case BREACHED -> "Breached " + hoursMinutes(remaining.isNegative() ? remaining.negated() : Duration.ZERO);
            case ON_TRACK, AT_RISK -> hoursMinutes(remaining) + " left";
        };
    }

    private static String hoursMinutes(Duration d)
    {
        return d.toHours() + "h " + d.toMinutesPart() + "m";
    }
}
