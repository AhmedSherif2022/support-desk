package com.supportdesk.ticket.dto;

import com.supportdesk.ticket.enums.QueueChip;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Agent home queue response with global open chip counts and filtered tickets in SLA risk order.")
public record QueueResponse(
        @Schema(description = "Active filter chip (defaults to ALL when omitted).", example = "ALL")
        QueueChip chip,
        @Schema(description = "How many open tickets sit on each chip (ALL, AT_RISK, CONFIRM_FIX, AUTO_CREATED), even when filtered.",
                example = "{\"ALL\": 4, \"AT_RISK\": 2, \"CONFIRM_FIX\": 1, \"AUTO_CREATED\": 0}")
        Map<String, Long> counts,
        @Schema(description = "Open tickets matching the selected chip: confirm-fix first, paused last, otherwise highest SLA % used first.")
        List<QueueTicketResponse> tickets)
{
}
