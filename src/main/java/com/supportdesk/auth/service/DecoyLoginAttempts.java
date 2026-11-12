package com.supportdesk.auth.service;
import com.supportdesk.auth.config.AuthProperties;
import com.supportdesk.auth.exception.AccountLockedException;
import com.supportdesk.auth.exception.InvalidCredentialsException;
import com.supportdesk.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Failed logins for usernames that do not exist (or are disabled). They count down and lock exactly like a
 * real account, so attemptsLeft and the 423 never reveal which usernames are real.
 */
@Component
@RequiredArgsConstructor
public class DecoyLoginAttempts
{
    // shortcut: kept in memory per app instance; move to the database or Redis when running more than one instance.
    private static final int MAX_TRACKED = 10_000;

    private record Attempts(Instant firstFailureAt, int failures, Instant lockedUntil)
    {
    }

    private final Map<String, Attempts> byUsername = new ConcurrentHashMap<>();
    private final AuthProperties properties;
    private final Clock clock;

    /** The exception to throw for this failed login: 401 with attemptsLeft, or 423 once locked. */
    public ApiException recordFailure(String username)
    {
        Instant now = clock.instant();
        if (byUsername.size() >= MAX_TRACKED) {
            byUsername.values().removeIf(a -> isStale(a, now));
        }
        Attempts after = byUsername.compute(username.trim().toLowerCase(Locale.ROOT), (key, before) -> next(before, now));
        if (after.lockedUntil() != null && after.lockedUntil().isAfter(now)) {
            return new AccountLockedException(after.lockedUntil(), Duration.between(now, after.lockedUntil()));
        }
        return new InvalidCredentialsException(properties.maxFailedAttempts() - after.failures());
    }

    /** Same rules as LoginAttemptService and User: count within the window, lock on the last one, ignore while locked. */
    private Attempts next(Attempts before, Instant now)
    {
        if (before != null && before.lockedUntil() != null && before.lockedUntil().isAfter(now)) {
            return before;
        }
        boolean freshWindow = before == null || before.firstFailureAt() == null
                || !now.isBefore(before.firstFailureAt().plus(properties.failureWindow()));
        Instant first = freshWindow ? now : before.firstFailureAt();
        int failures = (freshWindow ? 0 : before.failures()) + 1;
        if (failures >= properties.maxFailedAttempts()) {
            return new Attempts(null, 0, now.plus(properties.lockDuration()));
        }
        return new Attempts(first, failures, null);
    }

    private boolean isStale(Attempts a, Instant now)
    {
        boolean lockOver = a.lockedUntil() == null || !a.lockedUntil().isAfter(now);
        boolean windowOver = a.firstFailureAt() == null || !now.isBefore(a.firstFailureAt().plus(properties.failureWindow()));
        return lockOver && windowOver;
    }
}
