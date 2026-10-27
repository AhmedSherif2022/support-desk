package com.supportdesk.common.exception;

import com.supportdesk.common.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public class ApiException extends RuntimeException
{

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode)
    {
        this(errorCode, errorCode.getMessage());
    }

    // For messages that carry details, e.g. when a lock ends.
    public ApiException(ErrorCode errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus()
    {
        return errorCode.getStatus();
    }

    public String getCode()
    {
        return errorCode.name();
    }
}
