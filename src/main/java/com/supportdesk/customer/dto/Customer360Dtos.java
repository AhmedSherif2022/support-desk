package com.supportdesk.customer.dto;

import com.supportdesk.customer.enums.ActivityRange;
import com.supportdesk.customer.enums.ActivityResult;
import com.supportdesk.customer.enums.EnrollmentStatus;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.customer.enums.Segment;
import com.supportdesk.customer.enums.SensitiveField;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request and response bodies of customer search and Customer 360 (E2). Sensitive values are masked server-side. */
public final class Customer360Dtos
{
    private Customer360Dtos()
    {
    }

    // ---- E2-S1 search ----

    public record CustomerSearchResponse(
            List<CustomerSearchResult> results,
            @Schema(description = "Set only when nothing matched.", example = "No match. Check the digits and the ID type.")
            String message)
    {
    }

    public record CustomerSearchResult(
            @Schema(example = "CIF-100245") String cif,
            @Schema(example = "Sara Mahmoud") String fullName,
            @Schema(example = "PREMIUM") Segment segment,
            @Schema(example = "+20••••••4567") String maskedMobile,
            @Schema(example = "ENROLLED") EnrollmentStatus enrollmentStatus,
            @Schema(example = "2") long openTicketCount,
            @Schema(description = "Which identifier matched: CIF, NATIONAL_ID, MOBILE or TICKET.", example = "CIF")
            String matchedBy)
    {
    }

    // ---- E2-S2 / S3 Customer 360 header and reveal ----

    public record CustomerDetailDto(
            @Schema(example = "CIF-100245") String cif,
            @Schema(example = "Sara Mahmoud") String fullName,
            @Schema(example = "PREMIUM") Segment segment,
            @Schema(example = "••••••••••4567") String maskedNationalId,
            @Schema(example = "+20••••••4567") String maskedMobile,
            @Schema(example = "ENROLLED") EnrollmentStatus enrollmentStatus,
            @Schema(example = "AR") String preferredLanguage,
            @Schema(description = "When this agent verified the caller (\"Caller verified HH:MM\").") Instant verifiedAt)
    {
    }

    public record RevealRequest(
            @Schema(example = "MOBILE") @NotNull(message = "Choose the field to reveal.") SensitiveField field)
    {
    }

    public record RevealedFieldDto(
            @Schema(example = "MOBILE") SensitiveField field,
            @Schema(example = "+201001234567") String value)
    {
    }

    // ---- E2-S4 diagnosis ----

    public record DiagnosisDto(
            @Schema(description = "Look-back window in hours.", example = "6") int windowHours,
            @Schema(example = "true") boolean hasFailures,
            @Schema(description = "The most frequent failure, or null when there were none.") FailureSummary topFailure,
            @Schema(example = "3 failed LOGIN events with ERR_AUTH_503 on AUTHENTICATION in the last 6 hours.")
            String summary,
            @Schema(description = "Services with no failures in the window, so the agent can rule them out.",
                    example = "[\"OTP\", \"PAYMENTS\", \"TRANSFERS\"]")
            List<String> servicesWithoutFailures)
    {
    }

    public record FailureSummary(
            @Schema(example = "3") long count,
            @Schema(example = "LOGIN") String eventType,
            @Schema(example = "ERR_AUTH_503") String errorCode,
            @Schema(example = "AUTHENTICATION") String service,
            Instant firstAt,
            Instant lastAt)
    {
    }

    // ---- E2-S5 activity timeline ----

    public record ActivityResponse(
            @Schema(example = "LAST_6_HOURS") ActivityRange range,
            Instant from,
            Instant to,
            @Schema(example = "true") boolean failuresOnly,
            @Schema(description = "Counts for the time range and result filter, before the platform and service chips.")
            ActivityCounts counts,
            List<ActivityEventDto> events,
            @Schema(description = "Set only when no event matches.",
                    example = "No events match. Widen the time range, include successes, or clear the platform and service chips.")
            String emptyMessage)
    {
    }

    public record ActivityCounts(
            @Schema(example = "4") long total,
            @Schema(example = "3") long failures,
            @Schema(example = "{\"MOBILE\": 3, \"WEB\": 1}") Map<Platform, Long> byPlatform,
            @Schema(example = "{\"AUTHENTICATION\": 3, \"TRANSFERS\": 1}") Map<String, Long> byService)
    {
    }

    public record ActivityEventDto(
            Instant time,
            @Schema(example = "LOGIN") String eventType,
            @Schema(example = "MOBILE") Platform platform,
            @Schema(example = "AUTHENTICATION") String service,
            @Schema(example = "FAILURE") ActivityResult result,
            @Schema(example = "ERR_AUTH_503") String errorCode,
            @Schema(description = "True for failures, which the screen marks red.", example = "true") boolean failure)
    {
    }

    // ---- E2-S6 customer tickets ----

    public record CustomerTicketsDto(
            @Schema(description = "More than one ticket in the last 20 days.", example = "true") boolean repeatCaller,
            @Schema(example = "Repeat caller: 3 tickets in the last 20 days.") String repeatCallerNote,
            List<CustomerTicketRow> tickets)
    {
    }

    public record CustomerTicketRow(
            @Schema(example = "#2001") String number,
            @Schema(example = "Card payment declined") String title,
            @Schema(example = "Payments / Payment Failed") String category,
            @Schema(description = "Manual, Auto-created or From incident.", example = "MANUAL") TicketOrigin origin,
            @Schema(example = "TROUBLESHOOTING") TicketStatus status,
            @Schema(example = "HIGH") TicketPriority priority,
            @Schema(example = "AT_RISK") SlaState slaState,
            Instant createdAt)
    {
    }
}
