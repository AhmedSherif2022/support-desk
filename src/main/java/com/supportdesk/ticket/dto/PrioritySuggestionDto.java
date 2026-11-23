package com.supportdesk.ticket.dto;

import com.supportdesk.ticket.enums.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/** The priority to pre-select on the create-ticket form for one sub-category, and why. */
public record PrioritySuggestionDto(
        @Schema(example = "OTP_NOT_RECEIVED") String categoryCode,
        @Schema(example = "OTP / OTP Not Received") String category,
        @Schema(description = "Pre-select this priority; the agent can still change it.", example = "HIGH")
        TicketPriority suggestedPriority,
        @Schema(description = "The sub-category's own default priority.", example = "MEDIUM") TicketPriority categoryDefault,
        @Schema(description = "HISTORY: most agents chose this priority for recent tickets in this sub-category. "
                + "CATEGORY_DEFAULT: the sub-category's default (too few recent tickets, or no clear majority).",
                example = "HISTORY") Basis basedOn,
        @Schema(example = "Agents chose HIGH for 3 of 4 tickets in OTP Not Received in the last 90 days "
                + "(default MEDIUM).") String reason,
        @Schema(description = "SLA target of the suggested priority, in minutes.", example = "480") long slaTargetMinutes,
        @Schema(description = "Manual tickets in this sub-category in the last 90 days, by priority.",
                example = "{\"HIGH\":3,\"MEDIUM\":1}") Map<TicketPriority, Long> recentTickets)
{
    @Schema(enumAsRef = true)
    public enum Basis
    {
        HISTORY,
        CATEGORY_DEFAULT
    }
}
