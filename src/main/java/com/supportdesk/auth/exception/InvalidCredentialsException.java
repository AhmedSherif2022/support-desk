package com.supportdesk.auth.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;
import lombok.Getter;


@Getter
public class InvalidCredentialsException extends ApiException
{
    private final int attemptsLeft;

    public InvalidCredentialsException(int attemptsLeft)
    {
        super(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getMessage() + " " + attemptsLeft
                + (attemptsLeft == 1 ? " attempt" : " attempts") + " left before the account is locked.");
        this.attemptsLeft = attemptsLeft;
    }
}
