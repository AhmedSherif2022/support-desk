package com.supportdesk.ticket.enums;

import com.supportdesk.auth.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

/** Who takes a step. AGENT and REVIEWER act through the API; GUIDE and SYSTEM only from code. */
@Schema(enumAsRef = true, description = "Who takes a step: AGENT, GUIDE (a finished guide), REVIEWER or SYSTEM.")
public enum StepActor
{
    AGENT,
    GUIDE,
    REVIEWER,
    SYSTEM;

    /** The actor a logged-in user acts as. Null for GUIDE_ADMIN, who takes no ticket steps. */
    public static StepActor of(Role role)
    {
        return switch (role) {
            case AGENT -> AGENT;
            case REVIEWER -> REVIEWER;
            case GUIDE_ADMIN -> null;
        };
    }
}
