package com.supportdesk.auth.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;


public class PasswordNotHashedException extends ApiException
{
    public PasswordNotHashedException()
    {
        super(ErrorCode.PASSWORD_NOT_HASHED);
    }
}
