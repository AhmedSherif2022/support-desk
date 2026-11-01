package com.supportdesk.ticket.dto;
import com.supportdesk.customer.entity.Platform;
import com.supportdesk.ticket.entity.SlaState;
import com.supportdesk.ticket.entity.TicketOrigin;
import com.supportdesk.ticket.entity.TicketPriority;
import com.supportdesk.ticket.entity.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;


public final class TicketDtos
{
    private TicketDtos()
    {
    }

    public record CreateTicketRequest(
            @NotBlank(message = "Choose the customer.") String cif,
            @NotBlank(message = "Choose a category.") String categoryCode,   // a sub-category code
            TicketPriority priority,                                          // empty = the category's default
            @Size(max = 150) String title,                                    // empty = the category name
            @NotBlank(message = "Describe the problem.") @Size(max = 2000) String description,
            @Size(max = 40) String reference,                                 // TRX-... when the category needs it
            @Size(max = 40) String errorCode,                                 // empty = the category's default code
            @NotNull(message = "Choose the platform.") Platform platform,
            boolean allowDuplicate)                                           // true = create even if one is open
    {
    }


    public record PrecheckRequest(@NotBlank String cif, @NotBlank String categoryCode)
    {
    }


    public record ChangeStatusRequest(@NotNull(message = "Choose the new status.") TicketStatus status,
                                      @Size(max = 500) String reason)
    {
    }




    public record PrecheckDto(List<Warning> warnings)
    {

        public record Warning(String code, String message, String existingTicketNumber)
        {
        }
    }

    public record TicketDto(Long id, String number, String title, String description,
                            String customerCif, String customerName,
                            String category, String categoryCode, TicketPriority priority,
                            TicketStatus status, TicketOrigin origin, Platform platform,
                            String errorCode, String reference, String assignedAgent,
                            SlaState slaState, Instant slaDueAt, Set<TicketStatus> allowedNextStatuses,
                            int reopenCount, Instant createdAt)
    {
    }
}
