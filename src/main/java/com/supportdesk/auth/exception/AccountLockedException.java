package com.supportdesk.auth.exception;
import com.supportdesk.common.exception.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;


@Getter
public class AccountLockedException extends ApiException
{

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil)
    {
        super(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                "Your account is locked because of too many failed logins. Try again after " + lockedUntil + ".");
        this.lockedUntil = lockedUntil;
    }

}
