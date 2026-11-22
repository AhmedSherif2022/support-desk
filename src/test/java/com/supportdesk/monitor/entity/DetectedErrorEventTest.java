package com.supportdesk.monitor.entity;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.monitor.enums.EventOutcome;
import com.supportdesk.ticket.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DetectedErrorEventTest
{
    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private DetectedErrorEvent event;

    @BeforeEach
    void setUp()
    {
        event = new DetectedErrorEvent(mock(Customer.class), "  err_auth_503 ", Platform.MOBILE, "AUTH",
                "503 from auth", "corr-1", NOW.minusSeconds(60));
    }

    @Test
    void newRowIsNotProcessedAndCodeIsCleaned()
    {
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getOutcome()).isNull();
        assertThat(event.getTicket()).isNull();
        assertThat(event.getErrorCode()).isEqualTo("ERR_AUTH_503");    // trimmed + upper case
    }

    @Test
    void markProcessedSavesOutcomeTicketAndTime()
    {
        Ticket ticket = mock(Ticket.class);

        event.markProcessed(EventOutcome.CREATED, ticket, NOW);

        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getOutcome()).isEqualTo(EventOutcome.CREATED);
        assertThat(event.getTicket()).isSameAs(ticket);
        assertThat(event.getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    void unknownCodeIsProcessedWithoutATicket()
    {
        event.markProcessed(EventOutcome.UNKNOWN_CODE, null, NOW);

        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getTicket()).isNull();
    }

    @Test
    void aRowCanNeverBeProcessedTwice()
    {
        event.markProcessed(EventOutcome.CREATED, mock(Ticket.class), NOW);

        assertThatThrownBy(() -> event.markProcessed(EventOutcome.DUPLICATE, mock(Ticket.class), NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThat(event.getOutcome()).isEqualTo(EventOutcome.CREATED);   // first result kept
    }

    @Test
    void groupingChangesOnlyTheOutcome()
    {
        Ticket ticket = mock(Ticket.class);
        event.markProcessed(EventOutcome.CREATED, ticket, NOW);

        event.markGrouped();

        assertThat(event.getOutcome()).isEqualTo(EventOutcome.GROUPED);
        assertThat(event.getTicket()).isSameAs(ticket);
        assertThat(event.getProcessedAt()).isEqualTo(NOW);
    }
}
