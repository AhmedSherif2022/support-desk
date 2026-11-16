package com.supportdesk.ticket.enums;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(enumAsRef = true, description = "How the ticket was created.\n"
        + "- MANUAL: by an agent\n"
        + "- AUTO_CREATED: by the system from a detected error (automatic tickets are not built yet)\n"
        + "- FROM_INCIDENT: linked to a known incident (incidents are not built yet)")
public enum TicketOrigin
{
    MANUAL,
    AUTO_CREATED,
    FROM_INCIDENT
}
