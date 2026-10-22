package com.supportdesk.common.exception;


import org.springframework.http.HttpStatus;


public class UnauthenticatedException extends ApiException
{

    public UnauthenticatedException()
    {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please log in to continue.");
    }
}