package com.supportdesk.ticket.service;

import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.ticket.dto.PrioritySuggestionDto;
import com.supportdesk.ticket.dto.PrioritySuggestionDto.Basis;
import com.supportdesk.ticket.entity.TicketCategory;
import com.supportdesk.ticket.enums.TicketPriority;
import com.supportdesk.ticket.exception.CategoryNotFoundException;
import com.supportdesk.ticket.repository.TicketCategoryRepository;
import com.supportdesk.ticket.repository.TicketRepository;
import com.supportdesk.ticket.repository.TicketRepository.PriorityCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PrioritySuggestionServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

    private final TicketCategoryRepository categories = mock(TicketCategoryRepository.class);
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final SlaService slaService = mock(SlaService.class);
    private final PrioritySuggestionService service = new PrioritySuggestionService(categories, tickets, slaService,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private TicketCategory otp;

    @BeforeEach
    void setUp()
    {
        TicketCategory parent = category(1L, "OTP", "OTP", null, TicketPriority.MEDIUM);
        otp = category(2L, "OTP_NOT_RECEIVED", "OTP Not Received", parent, TicketPriority.MEDIUM);
        when(categories.findByCodeIgnoreCase("OTP_NOT_RECEIVED")).thenReturn(Optional.of(otp));
        when(categories.findByCodeIgnoreCase("OTP")).thenReturn(Optional.of(parent));
        when(slaService.targetFor(TicketPriority.HIGH)).thenReturn(Duration.ofHours(8));
        when(slaService.targetFor(TicketPriority.MEDIUM)).thenReturn(Duration.ofHours(24));
    }

    @Test
    void withNoHistoryTheCategoryDefaultIsSuggested()
    {
        history();

        PrioritySuggestionDto s = service.suggest("OTP_NOT_RECEIVED");

        assertThat(s.suggestedPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(s.basedOn()).isEqualTo(Basis.CATEGORY_DEFAULT);
        assertThat(s.reason()).contains("only 0 recent tickets");
        assertThat(s.slaTargetMinutes()).isEqualTo(24 * 60);
        assertThat(s.category()).isEqualTo("OTP / OTP Not Received");
    }

    @Test
    void aClearMajorityOfRecentTicketsWins()
    {
        history(count(TicketPriority.HIGH, 3), count(TicketPriority.MEDIUM, 1));

        PrioritySuggestionDto s = service.suggest(" OTP_NOT_RECEIVED ");   // trimmed; the repository ignores case

        assertThat(s.suggestedPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(s.basedOn()).isEqualTo(Basis.HISTORY);
        assertThat(s.categoryDefault()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(s.reason()).isEqualTo("Agents chose HIGH for 3 of 4 tickets in OTP Not Received in the last 90 days (default MEDIUM).");
        assertThat(s.slaTargetMinutes()).isEqualTo(8 * 60);
        assertThat(s.recentTickets()).containsEntry(TicketPriority.HIGH, 3L).containsEntry(TicketPriority.MEDIUM, 1L);
        verify(tickets).countManualPrioritiesSince(eq(2L), eq(NOW.minus(Duration.ofDays(90))));
    }

    @Test
    void halfIsNotAMajoritySoTheDefaultStays()
    {
        history(count(TicketPriority.HIGH, 2), count(TicketPriority.LOW, 2));

        PrioritySuggestionDto s = service.suggest("OTP_NOT_RECEIVED");

        assertThat(s.suggestedPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(s.basedOn()).isEqualTo(Basis.CATEGORY_DEFAULT);
        assertThat(s.reason()).contains("no single priority");
    }

    @Test
    void tooFewTicketsAreNotEnoughEvenIfTheyAgree()
    {
        history(count(TicketPriority.HIGH, 2));

        assertThat(service.suggest("OTP_NOT_RECEIVED").suggestedPriority()).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    void topLevelCategoryAndUnknownCodeAreRefused()
    {
        assertThatThrownBy(() -> service.suggest("OTP"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SUBCATEGORY_REQUIRED");
        assertThatThrownBy(() -> service.suggest("NOPE")).isInstanceOf(CategoryNotFoundException.class);
        verify(tickets, never()).countManualPrioritiesSince(any(), any());
    }


    private void history(PriorityCount... rows)
    {
        when(tickets.countManualPrioritiesSince(any(), any())).thenReturn(Arrays.asList(rows));
    }

    private static PriorityCount count(TicketPriority priority, long n)
    {
        return new PriorityCount()
        {
            public TicketPriority getPriority() { return priority; }
            public long getTickets() { return n; }
        };
    }

    private static TicketCategory category(Long id, String code, String name, TicketCategory parent, TicketPriority p)
    {
        TicketCategory c = new TicketCategory(code, name, parent, p, false, null, null, null);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
