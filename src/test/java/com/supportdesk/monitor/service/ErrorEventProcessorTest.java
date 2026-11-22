package com.supportdesk.monitor.service;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.monitor.entity.ErrorRule;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.monitor.event.AutoTicketCreatedEvent;
import com.supportdesk.ticket.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErrorEventProcessorTest
{
    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private final ErrorRuleService ruleService = mock(ErrorRuleService.class);
    private final AutoTicketService autoTicketService = mock(AutoTicketService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private ErrorEventProcessor processor;
    private DetectedErrorEvent event;

    @BeforeEach
    void setUp()
    {
        processor = new ErrorEventProcessor(ruleService, autoTicketService, events, Clock.fixed(NOW, ZoneOffset.UTC));
        event = new DetectedErrorEvent(mock(Customer.class), "ERR_AUTH_503", Platform.MOBILE, "AUTH",
                "503 from auth", "corr-1", NOW.minusSeconds(60));
    }

    @Test
    void unknownCodeMakesNoTicket()
    {
        when(ruleService.ruleFor("ERR_AUTH_503")).thenReturn(Optional.empty());

        assertThat(processor.process(event)).isEqualTo(EventOutcome.UNKNOWN_CODE);
        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getTicket()).isNull();
        verify(autoTicketService, never()).create(any(), any(), any());
    }

    @Test
    void openTicketForTheSameCodeIsJoinedNotDuplicated()
    {
        Ticket open = mock(Ticket.class);
        when(ruleService.ruleFor("ERR_AUTH_503")).thenReturn(Optional.of(mock(ErrorRule.class)));
        when(autoTicketService.findOpenDuplicate(event, NOW)).thenReturn(Optional.of(open));

        assertThat(processor.process(event)).isEqualTo(EventOutcome.DUPLICATE);
        assertThat(event.getTicket()).isSameAs(open);
        verify(autoTicketService).joinTimeline(open, event);
        verify(autoTicketService, never()).create(any(), any(), any());
    }

    @Test
    void knownCodeWithNoOpenTicketCreatesOneAndTellsMember2()
    {
        ErrorRule rule = mock(ErrorRule.class);
        Ticket created = mock(Ticket.class);
        when(ruleService.ruleFor("ERR_AUTH_503")).thenReturn(Optional.of(rule));
        when(autoTicketService.findOpenDuplicate(event, NOW)).thenReturn(Optional.empty());
        when(autoTicketService.create(event, rule, NOW)).thenReturn(created);

        assertThat(processor.process(event)).isEqualTo(EventOutcome.CREATED);
        assertThat(event.getTicket()).isSameAs(created);
        verify(events).publishEvent(any(AutoTicketCreatedEvent.class));
    }

    @Test
    void aRowCanNeverBeProcessedTwice()
    {
        when(ruleService.ruleFor("ERR_AUTH_503")).thenReturn(Optional.empty());
        processor.process(event);

        assertThatThrownBy(() -> processor.process(event)).isInstanceOf(IllegalStateException.class);
    }
}
