package com.supportdesk.ticket.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sort orders of the home queue (E3-S4). SLA is the default. Paused tickets are last in both.
 */
@Schema(enumAsRef = true, description = "Sort order of the home queue. Paused tickets are last in both.\n"
        + "- SLA (default): the highest SLA % used first, then the least time left, then the lower id\n"
        + "- PRIORITY: CRITICAL, HIGH, MEDIUM, then LOW; ties inside a priority follow the SLA order")
public enum QueueSort
{
    SLA,
    PRIORITY
}
