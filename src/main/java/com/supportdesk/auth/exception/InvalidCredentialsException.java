package com.supportdesk.auth.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;


public class InvalidCredentialsException extends ApiException
{
    public InvalidCredentialsException()
    {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
