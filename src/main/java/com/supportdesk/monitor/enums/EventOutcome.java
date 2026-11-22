package com.supportdesk.monitor.enums;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(enumAsRef = true, description = "What the monitor job did with one detected error row.\n"
        + "- `CREATED`: a new AUTO_CREATED ticket was made from the error rule.\n"
        + "- `DUPLICATE`: the customer already had an open ticket for this error code; the row joined its timeline.\n"
        + "- `GROUPED`: the ticket was linked to an incident (set by the incident grouping, Member 2).\n"
        + "- `UNKNOWN_CODE`: no rule for this error code; no ticket now (goes to Triage in E8).\n"
        + "- `FAILED`: processing threw; the row was set aside so it does not block the rows behind it.")
public enum EventOutcome
{
    CREATED,
    DUPLICATE,
    GROUPED,
    UNKNOWN_CODE,
    FAILED
}
