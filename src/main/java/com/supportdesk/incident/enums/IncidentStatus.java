package com.supportdesk.incident.enums;
import io.swagger.v3.oas.annotations.media.Schema;

// Internal lifecycle only. E5 (Azure DevOps) is postponed: nothing here is synchronised with a work item.
@Schema(enumAsRef = true, description = "Where a grouped outage stands.\n"
        + "- OPEN: customers are still hitting it; new matching tickets join this incident\n"
        + "- SRE_FIXED: SRE reported a fix\n"
        + "- RESOLVED: confirmed over")
public enum IncidentStatus
{
    OPEN,
    SRE_FIXED,
    RESOLVED
}
