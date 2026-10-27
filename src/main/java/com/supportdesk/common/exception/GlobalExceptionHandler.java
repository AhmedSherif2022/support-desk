package com.supportdesk.common.exception;
import com.supportdesk.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{

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
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.withFields(code.name(), code.getMessage(), fieldErrors, request.getRequestURI()));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return error(ErrorCode.FORBIDDEN, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(ErrorCode.MALFORMED_REQUEST, request);
    }

    // A unique or foreign-key constraint hit in the database, e.g. a duplicate username, email or mobile number.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data conflict on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return error(ErrorCode.DATA_CONFLICT, request);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Spring's own web errors (wrong HTTP method, unknown URL...) already know their status.
        if (ex instanceof ErrorResponse errorResponse) {
            return ResponseEntity
                    .status(errorResponse.getStatusCode())
                    .body(ApiError.of(ErrorCode.REQUEST_ERROR.name(), ErrorCode.REQUEST_ERROR.getMessage(),
                            request.getRequestURI()));
        }
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return error(ErrorCode.INTERNAL_ERROR, request);
    }

    private static ResponseEntity<ApiError> error(ErrorCode code, HttpServletRequest request) {
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code.name(), code.getMessage(), request.getRequestURI()));
    }
}
