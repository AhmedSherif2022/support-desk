package com.supportdesk.ticket.service;

import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.enums.Platform;
import com.supportdesk.customer.service.CallerVerificationService;
import com.supportdesk.customer.service.CustomerLookupService;
import com.supportdesk.ticket.config.TicketProperties;
import com.supportdesk.ticket.dto.TicketDtos.ChangeStatusRequest;
import com.supportdesk.ticket.dto.TicketDtos.CreateTicketRequest;
import com.supportdesk.ticket.dto.TicketDtos.TicketDto;
import com.supportdesk.ticket.enums.SlaState;
import com.supportdesk.ticket.entity.Ticket;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketOrigin;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.enums.TicketStatus;
import com.supportdesk.ticket.event.TicketEvents.TicketCreatedEvent;
import com.supportdesk.ticket.event.TicketEvents.TicketStatusChangedEvent;
import com.supportdesk.ticket.event.TicketEvents.TicketTransitionRejectedEvent;
import com.supportdesk.ticket.exception.DuplicateTicketException;
import com.supportdesk.ticket.exception.InvalidStatusTransitionException;
import com.supportdesk.ticket.exception.StatusSetBySystemException;
import com.supportdesk.ticket.exception.TicketNotFoundException;
import com.supportdesk.ticket.repository.TicketCategoryRepository;
import com.supportdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** TicketService with fake (mock) repositories and services: no Spring, no database. */
class TicketServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final TicketCategoryRepository categoryRepository = mock(TicketCategoryRepository.class);
    private final CustomerLookupService customerLookup = mock(CustomerLookupService.class);
    private final CallerVerificationService verificationService = mock(CallerVerificationService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final SlaService slaService = new SlaService(new TicketProperties(new TicketProperties.Sla(80, Map.of(
            TicketPriority.CRITICAL, Duration.ofHours(2),
            TicketPriority.HIGH, Duration.ofHours(8),
            TicketPriority.MEDIUM, Duration.ofHours(24),
            TicketPriority.LOW, Duration.ofHours(72)))), clock);

    private final TicketService service = new TicketService(ticketRepository, categoryRepository,
            new TicketValidator(), new TicketStatusMachine(), customerLookup, verificationService,
            currentUserService, slaService, events, clock,
            mock(com.supportdesk.customer.repository.CustomerActivityRepository.class),
            mock(com.supportdesk.guide.service.GuideMatcher.class));

    private final Customer sara = mock(Customer.class);
    private final User mona = mock(User.class);
    private final TicketCategory otp = new TicketCategory("OTP_NOT_RECEIVED", "OTP not received",
            new TicketCategory("OTP", "OTP", null, TicketPriority.MEDIUM, false, null, null, null),
            TicketPriority.HIGH, false, null, null, "ERR_OTP_TIMEOUT");

    @BeforeEach
    void setUp()
    {
        when(sara.getCif()).thenReturn("CIF-100245");
        when(sara.getFullName()).thenReturn("Sara");
        when(customerLookup.getByCif("CIF-100245")).thenReturn(sara);
        when(categoryRepository.findByCodeIgnoreCase("OTP_NOT_RECEIVED")).thenReturn(Optional.of(otp));
        when(currentUserService.getCurrentUserEntity()).thenReturn(mona);
        when(ticketRepository.nextTicketNumber()).thenReturn(2100L);
    }

    private CreateTicketRequest request(boolean allowDuplicate)
    {
        return new CreateTicketRequest("CIF-100245", "OTP_NOT_RECEIVED", null, null,
                "Customer did not get the OTP", null, null, Platform.MOBILE, allowDuplicate);
    }

    private Ticket ticket(TicketStatus status)
    {
        Ticket t = new Ticket(2001L, sara, otp, "OTP not received", "desc", TicketPriority.HIGH,
                TicketOrigin.MANUAL, Platform.MOBILE, null, null, mona, NOW, NOW.plus(Duration.ofHours(8)));
        if (status != TicketStatus.NEW) {
            t.moveTo(TicketStatus.RESOLVED, NOW);
            if (status == TicketStatus.CLOSED) {
                t.moveTo(TicketStatus.CLOSED, NOW);
            }
        }
        return t;
    }

    @Test
    void createSavesANewTicketWithDefaultsAndSla()
    {
        TicketDto dto = service.create(request(false));

        assertThat(dto.number()).isEqualTo("#2100");
        assertThat(dto.status()).isEqualTo(TicketStatus.NEW);
        assertThat(dto.priority()).isEqualTo(TicketPriority.HIGH);            // category default
        assertThat(dto.title()).isEqualTo("OTP not received");                // category name
        assertThat(dto.errorCode()).isEqualTo("ERR_OTP_TIMEOUT");             // category default code
        assertThat(dto.slaDueAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));  // HIGH = 8h
        assertThat(dto.slaState()).isEqualTo(SlaState.ON_TRACK);
        verify(verificationService).requireVerified(sara);
        verify(ticketRepository).save(any(Ticket.class));
        verify(events).publishEvent(any(TicketCreatedEvent.class));
    }

    @Test
    void createRefusesADuplicateOpenTicket()
    {
        Ticket open = ticket(TicketStatus.NEW);
        when(ticketRepository.findFirstByCustomerIdAndCategoryIdAndStatusNotIn(any(), any(), anyCollection()))
                .thenReturn(Optional.of(open));

        assertThatThrownBy(() -> service.create(request(false)))
                .isInstanceOf(DuplicateTicketException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void changeStatusMovesTheTicketAndPublishesTheChange()
    {
        when(ticketRepository.findByTicketNumber(2001L)).thenReturn(Optional.of(ticket(TicketStatus.NEW)));

        TicketDto dto = service.changeStatus(2001L, new ChangeStatusRequest(TicketStatus.RESOLVED, "fixed"));

        assertThat(dto.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(dto.slaState()).isEqualTo(SlaState.MET);
        verify(events).publishEvent(any(TicketStatusChangedEvent.class));
    }

    @Test
    void wrongMoveIsRejectedAndPublished()
    {
        when(ticketRepository.findByTicketNumber(2001L)).thenReturn(Optional.of(ticket(TicketStatus.CLOSED)));

        assertThatThrownBy(() -> service.changeStatus(2001L, new ChangeStatusRequest(TicketStatus.RESOLVED, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(events).publishEvent(any(TicketTransitionRejectedEvent.class));
    }

    @Test
    void agentCannotSetASystemStatus()
    {
        when(ticketRepository.findByTicketNumber(2001L)).thenReturn(Optional.of(ticket(TicketStatus.NEW)));

        assertThatThrownBy(() -> service.changeStatus(2001L, new ChangeStatusRequest(TicketStatus.ESCALATED, null)))
                .isInstanceOf(StatusSetBySystemException.class);
    }

    @Test
    void unknownTicketIs404()
    {
        when(ticketRepository.findByTicketNumber(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(9999L)).isInstanceOf(TicketNotFoundException.class);
    }
}
