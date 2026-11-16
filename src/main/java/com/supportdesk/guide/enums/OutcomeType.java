package com.supportdesk.guide.enums;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "What a guide's outcome does to the ticket when the run is finished.\n"
        + "- RESOLVED: step GUIDE_SOLVED, to RESOLVED\n"
        + "- FOLLOW_UP: step GUIDE_FOLLOW_UP, to WAITING_ON_CUSTOMER (the SLA clock pauses)\n"
        + "- ESCALATE: step GUIDE_HAND_OVER, to ESCALATED")
public enum OutcomeType
{
    RESOLVED,
    ESCALATE,
    FOLLOW_UP
}
