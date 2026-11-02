package com.supportdesk.customer.service;

import com.supportdesk.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Small E2 rules without Spring: masking, search value parsing, and the search rate limit. */
class CustomerRulesTest
{
    // ---- E2-S3 masking ----

    @Test
    void mobileShowsTheFirst3AndLast4()
    {
        assertThat(Masking.mobile("+201001234567")).isEqualTo("+20••••••4567");
    }

    @Test
    void nationalIdShowsTheLast4()
    {
        assertThat(Masking.nationalId("29001011234567")).isEqualTo("••••••••••4567");
    }

    @Test
    void tooShortValuesAreFullyMasked()
    {
        assertThat(Masking.mobile("1234")).isEqualTo("••••");
        assertThat(Masking.nationalId(null)).isEqualTo("••••");
    }

    // ---- E2-S1 search value parsing ----

    @Test
    void egyptianMobileFormatsAllNormalise()
    {
        assertThat(CustomerSearchService.mobile("01001234567")).contains("+201001234567");
        assertThat(CustomerSearchService.mobile("201001234567")).contains("+201001234567");
        assertThat(CustomerSearchService.mobile("+20 100 123 4567")).contains("+201001234567");
        assertThat(CustomerSearchService.mobile("CIF-100245")).isEmpty();
    }

    @Test
    void ticketNumbersAcceptTheHashButNotLongIds()
    {
        assertThat(CustomerSearchService.ticketNumber("#2001")).contains(2001L);
        assertThat(CustomerSearchService.ticketNumber("2001")).contains(2001L);
        assertThat(CustomerSearchService.ticketNumber("29001011234567")).isEmpty();   // a national ID
    }

    // ---- E1-S4 rate limit ----

    @Test
    void the31stSearchIn10MinutesIsRefusedAndTheWindowSlides()
    {
        Instant t0 = Instant.parse("2026-11-02T10:00:00Z");
        MovableClock clock = new MovableClock(t0);
        SearchRateLimiter limiter = new SearchRateLimiter(clock);

        for (int i = 0; i < 30; i++) {
            limiter.check(7L);
        }
        assertThatThrownBy(() -> limiter.check(7L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("TOO_MANY_SEARCHES");

        limiter.check(8L);                                  // another agent has their own budget
        clock.now = t0.plus(Duration.ofMinutes(10).plusSeconds(1));
        limiter.check(7L);                                  // the first 30 have left the window
    }

    private static final class MovableClock extends Clock
    {
        private Instant now;

        MovableClock(Instant now)
        {
            this.now = now;
        }

        @Override
        public ZoneId getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return now;
        }
    }
}
