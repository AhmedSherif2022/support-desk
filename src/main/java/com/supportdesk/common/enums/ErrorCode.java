package com.supportdesk.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Every error the API can return: the code clients see (the enum name), its HTTP status and default message.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode
{
    // auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Username or password is incorrect."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Your account is locked because of too many failed logins."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Please log in to continue."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to do this."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is not valid. Please log in again."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh token has expired. Please log in again."),

    // request and server
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Some fields are not valid."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request body could not be read."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "This data conflicts with existing data."),
    REQUEST_ERROR(HttpStatus.BAD_REQUEST, "The request could not be handled."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");

    private final HttpStatus status;
    private final String message;
}
