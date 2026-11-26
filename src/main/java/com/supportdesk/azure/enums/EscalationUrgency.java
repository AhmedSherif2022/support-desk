package com.supportdesk.azure.enums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Schema(enumAsRef = true, description = "How urgent the escalation is. Sent to Azure DevOps as the work item priority "
        + "(1 = highest, 4 = lowest).\n"
        + "- `CRITICAL`: priority 1\n"
        + "- `HIGH`: priority 2\n"
        + "- `MEDIUM`: priority 3\n"
        + "- `LOW`: priority 4")
@Getter
@RequiredArgsConstructor
public enum EscalationUrgency
{
    LOW(4),
    MEDIUM(3),
    HIGH(2),
    CRITICAL(1);

    private final int azurePriority;
}
