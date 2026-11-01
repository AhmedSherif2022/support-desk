package com.supportdesk.customer.exception;

import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;


public class CallerNotVerifiedException extends ApiException
{
    public CallerNotVerifiedException()
    {
        super(ErrorCode.CALLER_NOT_VERIFIED);
    }
}
