package com.supportdesk.health.enums;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Status of one bank service on one platform.\n"
        + "- DOWN: an open incident still has a linked ticket the agent has not closed\n"
        + "- DEGRADED: at least the configured number of distinct customers errored in the window\n"
        + "- UNKNOWN: the monitor job has not run recently, and nothing above applies\n"
        + "- OPERATIONAL: no reported problem")
public enum ServiceHealthStatus
{
    OPERATIONAL,
    DEGRADED,
    DOWN,
    UNKNOWN
}
