package com.supportdesk.auth.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import lombok.Getter;

import java.time.Instant;


@Getter
public class AccountLockedException extends ApiException
{

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil)
    {
        super(ErrorCode.ACCOUNT_LOCKED,
                ErrorCode.ACCOUNT_LOCKED.getMessage() + " Try again after " + lockedUntil + ".");
        this.lockedUntil = lockedUntil;
    }

}
