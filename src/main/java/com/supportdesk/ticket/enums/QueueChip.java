package com.supportdesk.ticket.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Filter chips of the home queue (E3-S4). ALL is the whole open queue.
 */
@Schema(enumAsRef = true, description = "Filter chip of the home queue.\n"
        + "- ALL: every open ticket, of every agent\n"
        + "- AT_RISK: SLA at risk or breached\n"
        + "- IN_REVIEW: an SRE fix on its way to review or waiting for confirmation (SRE_FIXED rows show Wait for "
        + "review, IN_REVIEW rows Confirm fix); the screen's \"Confirm fix\" chip\n"
        + "- AUTO_CREATED: created by the system, not by an agent")
public enum QueueChip
{
    ALL,
    AT_RISK,
    IN_REVIEW,
    AUTO_CREATED
}
