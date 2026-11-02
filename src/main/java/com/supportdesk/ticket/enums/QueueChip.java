package com.supportdesk.ticket.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Filter chips of the home queue (E3-S4). ALL is the whole open queue.
 */
@Schema(description = "Queue filter chip: ALL, AT_RISK (at risk or breached), CONFIRM_FIX (SRE fixed it, call the customer) or AUTO_CREATED.")
public enum QueueChip
{
    ALL,
    AT_RISK,
    CONFIRM_FIX,
    AUTO_CREATED
}
