package com.supportdesk.monitor.service;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.monitor.entity.ErrorRule;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.event.AutoTicketCreatedEvent;
import com.supportdesk.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ErrorEventProcessor
{
    private final ErrorRuleService ruleService;
    private final AutoTicketService autoTicketService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public EventOutcome process(DetectedErrorEvent event)
    {
        Instant now = clock.instant();


        Optional<ErrorRule> rule = ruleService.ruleFor(event.getErrorCode());
        if (rule.isEmpty()) {
            event.markProcessed(EventOutcome.UNKNOWN_CODE, null, now);
            // TODO (E8-S1): create the ticket in Triage instead of skipping it.
            return EventOutcome.UNKNOWN_CODE;
        }


        Optional<Ticket> open = autoTicketService.findOpenDuplicate(event, now);
        if (open.isPresent()) {
            autoTicketService.joinTimeline(open.get(), event);
            event.markProcessed(EventOutcome.DUPLICATE, open.get(), now);
            return EventOutcome.DUPLICATE;
        }


        Ticket ticket = autoTicketService.create(event, rule.get(), now);
        event.markProcessed(EventOutcome.CREATED, ticket, now);


        events.publishEvent(new AutoTicketCreatedEvent(ticket.getId(), event.getId(), event.getErrorCode(),
                event.getPlatform(), rule.get().isAutoEscalate(), event.getOccurredAt()));
        return EventOutcome.CREATED;
    }
}
