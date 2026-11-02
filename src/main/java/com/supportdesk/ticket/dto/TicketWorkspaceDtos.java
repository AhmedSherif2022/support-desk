package com.supportdesk.ticket.dto;

import com.supportdesk.audit.dto.AuditLogResponse;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Bodies of the ticket workspace tabs (E3-S5, S6) and the ticket list (E3-S7). */
public final class TicketWorkspaceDtos
{
    private TicketWorkspaceDtos()
    {
    }

    public record ContactRequest(
            @Schema(example = "Told the customer the OTP issue is fixed and asked them to try again.")
            @NotBlank(message = "Write what you told the customer.") @Size(max = 2000) String note)
    {
    }

    public record ContactDto(
            @Schema(example = "1") Long id,
            @Schema(example = "Told the customer the OTP issue is fixed and asked them to try again.") String note,
            @Schema(example = "Mona") String author,
            Instant time)
    {
    }

    public record ContactsResponse(
            @Schema(description = "For the tab label, e.g. \"Customer contact (2)\".", example = "2") int count,
            @Schema(description = "Newest first.") List<ContactDto> items)
    {
    }

    public record HistoryResponse(
            @Schema(description = "For the tab label, e.g. \"Timeline (3)\".", example = "3") int count,
            @Schema(description = "Newest first.") List<AuditLogResponse> items)
    {
    }

    public record TicketListRow(
            @Schema(example = "#2001") String number,
            @Schema(example = "Card payment declined") String title,
            @Schema(example = "CIF-100245") String customerCif,
            @Schema(example = "Sara Mahmoud") String customerName,
            @Schema(example = "TROUBLESHOOTING") TicketStatus status,
            @Schema(example = "HIGH") TicketPriority priority,
            @Schema(example = "MANUAL") TicketOrigin origin,
            @Schema(example = "AT_RISK") SlaState slaState,
            Instant createdAt)
    {
    }
}
