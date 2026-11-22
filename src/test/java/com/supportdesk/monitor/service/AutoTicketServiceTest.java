package com.supportdesk.monitor.service;
import com.supportdesk.audit.enums.AuditAction;
import com.supportdesk.audit.service.AuditService;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.monitor.config.MonitorProperties;
import com.supportdesk.monitor.entity.DetectedErrorEvent;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.repository.TicketRepository;
import com.supportdesk.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class AutoTicketServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private final TicketService ticketService = mock(TicketService.class);
    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private AutoTicketService service;
    private DetectedErrorEvent event;

    @BeforeEach
    void setUp()
    {
        service = new AutoTicketService(ticketService, ticketRepository, auditService,
                new MonitorProperties(true, 100, Duration.ofHours(24)));
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(5L);
        event = new DetectedErrorEvent(customer, "PAY_TIMEOUT", Platform.WEB, "PAYMENTS", "timeout", "c1", NOW);
    }

    @Test
    void openTicketFromTheLast24HoursIsTheDuplicate()
    {
        Ticket recent = mock(Ticket.class);
        when(ticketRepository.findFirstByCustomerIdAndErrorCodeAndStatusNotInAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(5L), eq("PAY_TIMEOUT"), anyCollection(), eq(NOW.minus(Duration.ofHours(24)))))
                .thenReturn(Optional.of(recent));

        assertThat(service.findOpenDuplicate(event, NOW)).contains(recent);
    }

    @Test
    void noOpenTicketInTheWindowMeansNoDuplicate()
    {
        // Covers older open tickets too (manual or automatic): outside 24 h they are not joined.
        when(ticketRepository.findFirstByCustomerIdAndErrorCodeAndStatusNotInAndCreatedAtAfterOrderByCreatedAtDesc(
                any(), any(), anyCollection(), any())).thenReturn(Optional.empty());

        assertThat(service.findOpenDuplicate(event, NOW)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void joiningATicketIsAuditedAsASystemAction()
    {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getTicketNumber()).thenReturn(1042L);

        service.joinTimeline(ticket, event);

        ArgumentCaptor<Object> after = ArgumentCaptor.forClass(Object.class);
        verify(auditService).record(eq(AuditAction.ERROR_EVENT_JOINED), isNull(), eq("TICKET"), eq("1042"),
                isNull(), after.capture());
        assertThat((Map<String, Object>) after.getValue())
                .containsEntry("errorCode", "PAY_TIMEOUT")
                .containsEntry("platform", "WEB");
    }
}
