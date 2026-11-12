package com.supportdesk.auth.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;


@Getter
public class AccountLockedException extends ApiException
{

    private final Instant lockedUntil;
    private final long retryAfterSeconds;

    public AccountLockedException(Instant lockedUntil, Duration remaining)
    {
        super(ErrorCode.ACCOUNT_LOCKED, ErrorCode.ACCOUNT_LOCKED.getMessage() + " Try again in " + minutes(remaining) + ".");
        this.lockedUntil = lockedUntil;
        this.retryAfterSeconds = (remaining.toMillis() + 999) / 1000;   // round up: never say 0 while still locked
    }

    private static String minutes(Duration remaining)
    {
        long minutes = Math.max(1, (remaining.toSeconds() + 59) / 60);
        return minutes == 1 ? "1 minute" : minutes + " minutes";
    }

}
