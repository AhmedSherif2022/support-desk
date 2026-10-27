package com.supportdesk.auth.exception;
import com.supportdesk.common.enums.ErrorCode;
import com.supportdesk.common.exception.ApiException;


// Unknown refresh token: never issued, or deleted on logout.
public class InvalidRefreshTokenException extends ApiException
{
    public InvalidRefreshTokenException()
    {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
