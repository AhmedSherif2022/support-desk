package com.supportdesk.ticket.service;

import com.supportdesk.guide.service.GuideMatcher;
import com.supportdesk.ticket.enums.NextAction;
import com.supportdesk.ticket.enums.QueueChip;
import com.supportdesk.ticket.dto.QueueResponse;
import com.supportdesk.ticket.dto.QueueTicketResponse;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.repository.TicketRepository;
import com.supportdesk.ticket.dto.SlaSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The home queue (E3-S4): open tickets in risk order, with a count for every chip in the same call.
 */
@Service
@RequiredArgsConstructor
public class TicketQueryService
{
    private static final List<TicketStatus> CLOSED_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository tickets;
    private final SlaService slaService;
    private final GuideMatcher guideMatcher;

    @Transactional(readOnly = true)
    public QueueResponse queue(QueueChip chip)
    {
        QueueChip selected = chip == null ? QueueChip.ALL : chip;
        Instant now = slaService.now();
        Predicate<Ticket> hasGuide = guideMatcher.loadAvailability();
        // TODO (E8 triage): leave out tickets waiting for Guide Admin triage once that status exists.
        List<QueueTicketResponse> open = tickets.findByStatusNotIn(CLOSED_STATUSES).stream()
                .map(ticket -> toResponse(ticket, now, hasGuide.test(ticket)))
                .sorted(riskOrder())
                .toList();

        List<QueueTicketResponse> visible = open.stream()
                .filter(ticket -> matches(selected, ticket))
                .toList();

        return new QueueResponse(selected, counts(open), visible);
    }

    private QueueTicketResponse toResponse(Ticket ticket, Instant now, boolean guideAvailable)
    {
        SlaSnapshot snapshot = slaService.evaluate(ticket, now);
        NextAction next = NextAction.resolve(ticket.getStatus(), snapshot.state(), guideAvailable);
        return new QueueTicketResponse(
                ticket.getId(),
                ticket.displayNumber(),
                ticket.getTitle(),
                ticket.getCustomer() == null ? null : ticket.getCustomer().getCif(),
                ticket.getCustomer() == null ? null : ticket.getCustomer().getFullName(),
                ticket.getCustomer() == null ? null : ticket.getCustomer().getSegment(),
                ticket.getCategory() == null ? null : ticket.getCategory().fullName(),
                ticket.getPlatform(),
                ticket.getErrorCode(),
                ticket.getOrigin(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignedAgent() == null ? null : ticket.getAssignedAgent().getFullName(),
                snapshot.state(),
                snapshot.label(),
                ticket.getSlaDueAt(),
                snapshot.percentUsed(),
                snapshot.remaining().toSeconds(),
                snapshot.paused(),
                next,
                next == null ? null : next.label());
    }

    private static boolean matches(QueueChip chip, QueueTicketResponse ticket)
    {
        return switch (chip) {
            case ALL -> true;
            case AT_RISK -> ticket.slaState() == SlaState.AT_RISK || ticket.slaState() == SlaState.BREACHED;
            case CONFIRM_FIX -> ticket.status() == TicketStatus.SRE_FIXED;
            case AUTO_CREATED -> ticket.origin() == TicketOrigin.AUTO_CREATED;
        };
    }

    private static Map<String, Long> counts(List<QueueTicketResponse> open)
    {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (QueueChip chip : QueueChip.values()) {
            counts.put(chip.name(), open.stream().filter(ticket -> matches(chip, ticket)).count());
        }
        return Collections.unmodifiableMap(counts);
    }

    /**
     * "Confirm fix" tickets first (the customer is waiting for a call), paused tickets last (nothing to do yet),
     * and in between the highest percent used first, then the least time left, then id.
     */
    private static Comparator<QueueTicketResponse> riskOrder()
    {
        return Comparator.comparingInt(TicketQueryService::band)
                .thenComparing(Comparator.comparingInt(QueueTicketResponse::percentUsed).reversed())
                .thenComparingLong(QueueTicketResponse::remainingSeconds)
                .thenComparing(QueueTicketResponse::id, Comparator.nullsLast(Long::compareTo));
    }

    private static int band(QueueTicketResponse ticket)
    {
        if (ticket.status() == TicketStatus.SRE_FIXED) {
            return 0;
        }
        return ticket.paused() ? 2 : 1;
    }
}
