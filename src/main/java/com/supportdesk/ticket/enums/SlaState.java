package com.supportdesk.ticket.enums;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(enumAsRef = true, description = "Where a ticket stands against its SLA target (the target depends on the priority).\n"
        + "- ON_TRACK: under 75% of the target used (the warning line is configurable)\n"
        + "- AT_RISK: 75% or more used\n"
        + "- BREACHED: 100% or more used, open or not\n"
        + "- PAUSED: waiting on the customer, under 100%; the clock is stopped\n"
        + "- MET: resolved or closed within the target")
public enum SlaState
{
    ON_TRACK,
    AT_RISK,
    BREACHED,
    PAUSED,
    MET
}
