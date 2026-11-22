package com.supportdesk.monitor.enums;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "What started the monitor job run.\n"
        + "- `SCHEDULE`: the every-minute schedule.\n"
        + "- `MANUAL`: a Guide Admin pressed \"Run job now\" (E8-S4).")
public enum JobTrigger
{
    SCHEDULE,
    MANUAL
}
