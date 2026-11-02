package com.supportdesk.customer.service;

import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.service.CurrentUserService;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.customer.entity.CallerVerification;
import com.supportdesk.customer.entity.Customer;
import com.supportdesk.customer.entity.Segment;
import com.supportdesk.customer.entity.VerificationCheck;
import com.supportdesk.customer.exception.CallerNotVerifiedException;
import com.supportdesk.customer.repository.CallerVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Caller verification rules with mocks: no Spring, no database. */
class CallerVerificationServiceTest
{
    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private final CallerVerificationRepository repository = mock(CallerVerificationRepository.class);
    private final CustomerLookupService customerLookup = mock(CustomerLookupService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final CallerVerificationService service = new CallerVerificationService(
            repository, customerLookup, currentUserService, Clock.fixed(NOW, ZoneOffset.UTC));

    private final Customer sara = new Customer("CIF-100245", "Sara Mahmoud", "29001011234567",
            "+201001234567", LocalDate.of(1990, 1, 1), Segment.PREMIUM, "AR");
    private final User mona = mock(User.class);

    @BeforeEach
    void setUp()
    {
        when(customerLookup.getByCif("CIF-100245")).thenReturn(sara);
        when(currentUserService.getCurrentUserEntity()).thenReturn(mona);
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(mona.getId()).thenReturn(1L);
        when(repository.save(any(CallerVerification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void twoChecksVerifyTheCallerFor30Minutes()
    {
        var dto = service.verify("CIF-100245",
                Set.of(VerificationCheck.DATE_OF_BIRTH, VerificationCheck.MOBILE_ON_FILE));

        assertThat(dto.verified()).isTrue();
        assertThat(dto.expiresAt()).isEqualTo(NOW.plusSeconds(30 * 60));
        verify(repository).save(any(CallerVerification.class));
    }

    @Test
    void oneCheckIsNotEnough()
    {
        assertThatThrownBy(() -> service.verify("CIF-100245", Set.of(VerificationCheck.DATE_OF_BIRTH)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("At least two identity checks must pass.");
        verify(repository, never()).save(any());
    }

    @Test
    void requireVerifiedFailsWithoutAnActiveVerification()
    {
        when(repository.findFirstByAgentIdAndCustomerIdAndEndedAtIsNullAndExpiresAtAfterOrderByVerifiedAtDesc(
                anyLong(), any(), eq(NOW))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireVerified(sara)).isInstanceOf(CallerNotVerifiedException.class);
    }

    @Test
    void endedCallIsNoLongerActive()
    {
        CallerVerification v = new CallerVerification(sara, mona, Set.of(VerificationCheck.DATE_OF_BIRTH,
                VerificationCheck.MOBILE_ON_FILE), NOW, NOW.plusSeconds(1800));
        assertThat(v.isActive(NOW)).isTrue();
        v.end(NOW);
        assertThat(v.isActive(NOW)).isFalse();
    }
}
