package com.supportdesk.auth.service;

import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.enums.Role;
import com.supportdesk.auth.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** E1-S1: 5 failed attempts within 15 minutes lock the account for 15 minutes. */
class LoginAttemptServiceTest
{
    private static final Instant T0 = Instant.parse("2026-11-02T10:00:00Z");

    private final AuthProperties properties = new AuthProperties("secret", Duration.ofMinutes(15), 5,
            Duration.ofMinutes(15), Duration.ofMinutes(15), Duration.ofMinutes(30));

    private final User user = new User("mona", "hash", "Mona", "mona@test.local", "+201000000000", Role.AGENT);

    @Test
    void fiveFailuresWithinTheWindowLockFor15Minutes()
    {
        for (int minute = 0; minute < 5; minute++) {
            at(T0.plus(Duration.ofMinutes(minute))).recordFailure(user);
        }

        Instant fifthFailure = T0.plus(Duration.ofMinutes(4));
        assertThat(user.getLockedUntil()).isEqualTo(fifthFailure.plus(Duration.ofMinutes(15)));
        assertThat(user.isLocked(fifthFailure.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(user.isLocked(fifthFailure.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void failuresSpreadOverMoreThan15MinutesDoNotLock()
    {
        for (int minute = 0; minute < 4; minute++) {
            at(T0.plus(Duration.ofMinutes(minute))).recordFailure(user);
        }
        at(T0.plus(Duration.ofMinutes(16))).recordFailure(user);   // the first 4 are now outside the window

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void successfulLoginStartsTheCountAgain()
    {
        for (int minute = 0; minute < 4; minute++) {
            at(T0.plus(Duration.ofMinutes(minute))).recordFailure(user);
        }
        at(T0.plus(Duration.ofMinutes(5))).recordSuccess(user);
        at(T0.plus(Duration.ofMinutes(6))).recordFailure(user);

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    private LoginAttemptService at(Instant now)
    {
        return new LoginAttemptService(properties, Clock.fixed(now, ZoneOffset.UTC));
    }
}
