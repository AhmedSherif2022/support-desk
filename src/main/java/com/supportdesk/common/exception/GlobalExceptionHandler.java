package com.supportdesk.common.exception;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler
{

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiError.of(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.withFields("VALIDATION_FAILED", "Some fields are not valid.",
                        fieldErrors, request.getRequestURI()));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("FORBIDDEN", "You do not have permission to do this.", request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("MALFORMED_REQUEST", "The request body could not be read.",
                        request.getRequestURI()));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Spring's own web errors (wrong HTTP method, unknown URL...) already know their status.
        if (ex instanceof ErrorResponse errorResponse) {
            return ResponseEntity
                    .status(errorResponse.getStatusCode())
                    .body(ApiError.of("REQUEST_ERROR", "The request could not be handled.",
                            request.getRequestURI()));
        }
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Something went wrong. Please try again.",
                        request.getRequestURI()));
    }
}
