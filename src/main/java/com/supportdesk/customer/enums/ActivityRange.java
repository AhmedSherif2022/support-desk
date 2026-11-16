package com.supportdesk.customer.enums;
import io.swagger.v3.oas.annotations.media.Schema;

/** Time filter of the activity timeline (E2-S5). CUSTOM needs from and to. */
@Schema(enumAsRef = true, description = "Time filter of the activity timeline.\n"
        + "- LAST_6_HOURS: the default\n"
        + "- TODAY: since midnight\n"
        + "- LAST_7_DAYS\n"
        + "- CUSTOM: needs `from` and `to`, with `from` before `to`")
public enum ActivityRange
{
    LAST_6_HOURS,
    TODAY,
    LAST_7_DAYS,
    CUSTOM
}
