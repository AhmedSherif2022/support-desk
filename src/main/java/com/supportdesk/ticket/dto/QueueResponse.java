package com.supportdesk.ticket.dto;

import com.supportdesk.ticket.enums.QueueChip;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "The home queue: every agent's open tickets (shared by the team), with chip counts.")
public record QueueResponse(
        @Schema(description = "Active filter chip (defaults to ALL when omitted).", example = "ALL")
        QueueChip chip,
        @Schema(description = "How many open tickets sit on each chip (ALL, AT_RISK, IN_REVIEW, AUTO_CREATED), even when filtered.",
                example = "{\"ALL\": 4, \"AT_RISK\": 2, \"IN_REVIEW\": 1, \"AUTO_CREATED\": 0}")
        Map<String, Long> counts,
        @Schema(description = "Open tickets matching the chip, in queue order (see GET /api/tickets/queue). Every "
                + "match is returned (no paging).")
        List<QueueTicketResponse> tickets)
{
}
