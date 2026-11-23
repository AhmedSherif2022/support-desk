package com.supportdesk.incident.dto;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.incident.entity.Incident;
import com.supportdesk.incident.entity.IncidentTicket;
import com.supportdesk.incident.enums.IncidentStatus;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public final class IncidentDtos
{
    private IncidentDtos()
    {
    }

    /** One grouped outage as the Agent sees it. No customer data: the tickets endpoint lists who was hit. */
    public record IncidentDto(
            @Schema(example = "7") Long id,
            @Schema(example = "ERR_AUTH_503") String errorCode,
            @Schema(example = "MOBILE") Platform platform,
            @Schema(example = "AUTH") String service,
            @Schema(example = "OPEN") IncidentStatus status,
            @Schema(description = "Different customers behind the linked tickets.", example = "5") int affectedCustomerCount,
            @Schema(description = "Tickets linked to this incident.", example = "5") long ticketCount,
            @Schema(example = "2026-11-22T10:20:00Z") Instant firstOccurredAt,
            @Schema(example = "2026-11-22T10:31:00Z") Instant lastOccurredAt,
            @Schema(example = "2026-11-22T10:32:00Z") Instant createdAt,
            @Schema(example = "2026-11-22T10:32:00Z") Instant updatedAt,
            @Schema(types = {"string", "null"}) Instant resolvedAt)
    {
        public static IncidentDto from(Incident incident, long ticketCount)
        {
            return new IncidentDto(incident.getId(), incident.getErrorCode(), incident.getPlatform(),
                    incident.getService(), incident.getStatus(), incident.getAffectedCustomerCount(), ticketCount,
                    incident.getFirstOccurredAt(), incident.getLastOccurredAt(), incident.getCreatedAt(),
                    incident.getUpdatedAt(), incident.getResolvedAt());
        }
    }

    /** A ticket inside an incident. The customer is named by CIF and name only; masked fields stay in Customer 360. */
    public record IncidentTicketDto(
            @Schema(example = "2104") Long ticketNumber,
            @Schema(example = "Login failed (auth service unavailable)") String title,
            @Schema(example = "NEW") TicketStatus status,
            @Schema(example = "CRITICAL") TicketPriority priority,
            @Schema(example = "CIF-100249") String customerCif,
            @Schema(example = "Laila Hany") String customerName,
            @Schema(example = "LOGIN_FAILED") String categoryCode,
            @Schema(example = "2026-11-22T10:21:00Z") Instant createdAt,
            @Schema(example = "2026-11-22T10:32:00Z") Instant linkedAt)
    {
        public static IncidentTicketDto from(IncidentTicket link)
        {
            Ticket ticket = link.getTicket();
            return new IncidentTicketDto(ticket.getTicketNumber(), ticket.getTitle(), ticket.getStatus(),
                    ticket.getPriority(), ticket.getCustomer().getCif(), ticket.getCustomer().getFullName(),
                    ticket.getCategory().getCode(), ticket.getCreatedAt(), link.getLinkedAt());
        }
    }

    /** The grouping thresholds in force. */
    public record IncidentSettingsDto(
            @Schema(description = "Different customers needed before one incident is opened.", example = "5")
            int minAffectedCustomers,
            @Schema(description = "Their errors must fall within this many minutes of each other.", example = "15")
            int groupingWindowMinutes)
    {
    }

    public record UpdateIncidentSettingsRequest(
            @Schema(description = "Different customers needed before one incident is opened.", example = "5")
            @NotNull(message = "Give the minimum number of affected customers.")
            @Positive(message = "The minimum number of affected customers must be at least 1.")
            @Max(value = 10_000, message = "The minimum number of affected customers must be at most 10000.")
            Integer minAffectedCustomers,
            @Schema(description = "Grouping window in minutes.", example = "15")
            @NotNull(message = "Give the grouping window in minutes.")
            @Positive(message = "The grouping window must be at least 1 minute.")
            @Max(value = 1_440, message = "The grouping window must be at most 1440 minutes (one day).")
            Integer groupingWindowMinutes)
    {
    }
}
