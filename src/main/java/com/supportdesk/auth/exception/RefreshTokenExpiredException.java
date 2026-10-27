package com.supportdesk.auth.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;


public class RefreshTokenExpiredException extends ApiException
{
    public RefreshTokenExpiredException()
    {
        super(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}
