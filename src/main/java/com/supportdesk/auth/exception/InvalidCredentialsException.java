package com.supportdesk.auth.exception;
import com.supportdesk.common.exception.ApiException;
import org.springframework.http.HttpStatus;



public class InvalidCredentialsException extends ApiException
{
    public InvalidCredentialsException()
    {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Username or password is incorrect.");
    }
}
