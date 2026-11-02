package com.supportdesk.ticket.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/** Status filter of the ticket list (E3-S7). */
@Schema(description = "ALL, OPEN, or RESOLVED_CLOSED.")
public enum TicketListView
{
    ALL,
    OPEN,
    RESOLVED_CLOSED
}
