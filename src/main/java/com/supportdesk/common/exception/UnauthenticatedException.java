package com.supportdesk.common.exception;

import com.supportdesk.common.enums.ErrorCode;


public class UnauthenticatedException extends ApiException
{

    public UnauthenticatedException()
    {
        super(ErrorCode.UNAUTHORIZED);
    }
}
