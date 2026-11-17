package com.supportdesk.guide.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/** Where an agent's step report stands in the Guide Admin's queue. */
@Schema(enumAsRef = true, description = "Where a flagged step stands.\n"
        + "- OPEN: new, not handled yet\n"
        + "- RESOLVED: the Guide Admin handled it (Mark done is not built yet)")
public enum GuideFeedbackStatus
{
    OPEN,
    RESOLVED
}
