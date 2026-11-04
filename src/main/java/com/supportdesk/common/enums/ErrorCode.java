package com.supportdesk.common.enums;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode
{

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Username or password is incorrect."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Your account is locked because of too many failed logins."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Please log in to continue."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to do this."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is not valid. Please log in again."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh token has expired. Please log in again."),


    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Some fields are not valid."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request body could not be read."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "This data conflicts with existing data."),
    REQUEST_ERROR(HttpStatus.BAD_REQUEST, "The request could not be handled."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again."),

    // customers
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "This customer does not exist."),
    CALLER_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Verify the caller before doing this."),
    TOO_FEW_CHECKS(HttpStatus.UNPROCESSABLE_ENTITY, "Two checks must pass."),
    TOO_MANY_SEARCHES(HttpStatus.TOO_MANY_REQUESTS, "Too many searches. Wait a few minutes and try again."),
    CUSTOM_RANGE_REQUIRED(HttpStatus.BAD_REQUEST, "A custom range needs from and to, with from before to."),


    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "This ticket does not exist."),
    CATEGORY_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "This category does not exist."),
    DUPLICATE_OPEN_TICKET(HttpStatus.CONFLICT, "This customer already has an open ticket for this problem."),
    CATEGORY_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "This category is no longer used."),
    SUBCATEGORY_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Choose a sub-category."),
    REFERENCE_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Transaction reference must start with TRX-"),
    REFERENCE_FORMAT(HttpStatus.UNPROCESSABLE_ENTITY, "The transaction reference format is not valid."),
    INVALID_STATUS_TRANSITION(HttpStatus.UNPROCESSABLE_ENTITY, "This status change is not allowed."),
    STATUS_SET_BY_SYSTEM(HttpStatus.UNPROCESSABLE_ENTITY, "This status is set by the system, not by hand."),

    // guides
    GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "This guide does not exist or has no live version."),
    GUIDE_RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "This guide run does not exist."),
    GUIDE_RUN_ALREADY_ACTIVE(HttpStatus.CONFLICT, "This ticket already has a guide in progress."),
    GUIDE_RUN_NOT_ACTIVE(HttpStatus.CONFLICT, "This guide run is finished."),
    GUIDE_DOES_NOT_FIT(HttpStatus.UNPROCESSABLE_ENTITY, "This guide does not fit the ticket's platform, category or error."),
    INVALID_GUIDE_ROUTE(HttpStatus.UNPROCESSABLE_ENTITY, "This answer does not fit the current step.");

    private final HttpStatus status;
    private final String message;
}
