package com.supportdesk.common.exception;
import org.springframework.http.HttpStatus;
import com.supportdesk.common.enums.ErrorCode;

public class BusinessRuleException extends ApiException
{
    public BusinessRuleException(ErrorCode errorCode)
    {
        super(errorCode);
    }

    public BusinessRuleException(ErrorCode errorCode, String message)
    {
        super(errorCode, message);
    }
}
