package com.supportdesk.ticket.dto;

import com.supportdesk.auth.entity.User;
import com.supportdesk.ticket.enums.NextAction;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketStatus;

import java.util.Locale;
import java.util.Set;

public final class TicketMapper
{
    private TicketMapper()
    {
    }

    public static TicketDto toDto(Ticket t, Set<TicketStatus> allowedNext, SlaSnapshot sla, boolean guideAvailable)
    {
        NextAction next = NextAction.resolve(t.getStatus(), sla.state(), guideAvailable);
        return new TicketDto(t.getId(), t.displayNumber(), t.getTitle(), t.getDescription(),
                t.getCustomer().getCif(), t.getCustomer().getFullName(),
                t.getCategory().fullName(), t.getCategory().getCode(), t.getPriority(),
                t.getStatus(), t.getOrigin(), t.getPlatform(), t.getErrorCode(), t.getReference(),
                name(t.getAssignedAgent()), sla.state(), t.getSlaDueAt(), allowedNext,
                t.getReopenCount(), t.getCreatedAt(),
                sla.percentUsed(), sla.label(), next, next == null ? null : next.label());
    }

    public static String label(TicketStatus status)
    {
        String s = status.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String name(User user)
    {
        return user == null ? null : user.getFullName();
    }
}
