package com.supportdesk.health.dto;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.health.enums.ServiceHealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public final class ServiceHealthDtos
{
    private ServiceHealthDtos()
    {
    }

    /** The agent home panel: one row per fixed service on MOBILE and on WEB. */
    public record ServiceHealthResponse(
            @Schema(description = "True when the monitor job has not started or finished in the last 5 minutes. "
                    + "A row that would be OPERATIONAL is UNKNOWN instead, so a stopped job does not look healthy.")
            boolean dataStale,
            @Schema(description = "When this response was built.") Instant evaluatedAt,
            @Schema(description = "When the monitor job last started or finished. Null when it has never run.",
                    types = {"string", "null"}) Instant monitorLastActivityAt,
            @Schema(description = "The DEGRADED window in force, in minutes.", example = "15") int windowMinutes,
            @Schema(description = "Distinct customers in that window that make a row DEGRADED.", example = "5")
            int minCustomers,
            List<ServiceHealthRow> services)
    {
    }

    public record ServiceHealthRow(
            @Schema(example = "PAYMENTS") String service,
            @Schema(example = "MOBILE") Platform platform,
            ServiceHealthStatus status,
            @Schema(description = "Distinct customers with any error in the window.", example = "2")
            int distinctCustomers,
            @Schema(description = "Open incidents for this row that still have a ticket the agent has not closed.")
            List<Long> openIncidentIds,
            @Schema(description = "Open auto-created tickets that are CRITICAL or past their SLA. "
                    + "Shown so the agent notices them. They do not change status.")
            List<ServiceHealthNotice> notices)
    {
    }

    public record ServiceHealthNotice(
            @Schema(example = "2104") long ticketNumber,
            @Schema(description = "The ticket's error rule is CRITICAL.") boolean critical,
            @Schema(description = "The ticket has used 100% or more of its SLA target.") boolean slaBreached)
    {
    }

    public record ServiceHealthSettingsDto(
            @Schema(description = "Distinct customers in the window before a row is DEGRADED.", example = "5")
            int minCustomers,
            @Schema(description = "How many minutes back the customer count looks.", example = "15")
            int windowMinutes)
    {
    }

    public record UpdateServiceHealthSettingsRequest(
            @Schema(description = "Distinct customers in the window before a row is DEGRADED.", example = "5")
            @NotNull(message = "Give the minimum number of customers.")
            @Positive(message = "The minimum number of customers must be at least 1.")
            @Max(value = 10_000, message = "The minimum number of customers must be at most 10000.")
            Integer minCustomers,
            @Schema(description = "How many minutes back the customer count looks.", example = "15")
            @NotNull(message = "Give the degraded window in minutes.")
            @Positive(message = "The degraded window must be at least 1 minute.")
            @Max(value = 1_440, message = "The degraded window must be at most 1440 minutes (one day).")
            Integer windowMinutes)
    {
    }
}
