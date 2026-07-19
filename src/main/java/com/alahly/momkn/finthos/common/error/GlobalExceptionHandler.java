package com.alahly.momkn.finthos.common.error;

import com.alahly.momkn.finthos.common.config.CorrelationIdFilter;
import com.alahly.momkn.finthos.wallet.domain.InsufficientFundsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProcessorDeclinedException.class)
    public ResponseEntity<ApiError> handleProcessorDeclined(ProcessorDeclinedException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ApiError(HttpStatus.PAYMENT_REQUIRED.value(), "PROCESSOR_DECLINED",
                        ex.getMessage(), correlationId(request)));
    }

    @ExceptionHandler(ProcessorTimeoutException.class)
    public ResponseEntity<ApiError> handleProcessorTimeout(ProcessorTimeoutException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ApiError(HttpStatus.GATEWAY_TIMEOUT.value(), "PROCESSOR_TIMEOUT",
                        ex.getMessage(), correlationId(request)));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(EmailAlreadyExistsException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(HttpStatus.CONFLICT.value(), "EMAIL_ALREADY_EXISTS",
                        ex.getMessage(), correlationId(request)));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleDuplicateUsername(UsernameAlreadyExistsException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(HttpStatus.CONFLICT.value(), "USERNAME_ALREADY_EXISTS",
                        ex.getMessage(), correlationId(request)));
    }

    @ExceptionHandler({AuthenticationFailedException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiError> handleAuthFailure(RuntimeException ex,
                                                      HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                        "Invalid email or password", correlationId(request)));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex,
                                                            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(HttpStatus.UNPROCESSABLE_ENTITY.value(), "INSUFFICIENT_FUNDS",
                        ex.getMessage(), correlationId(request)));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(HttpStatus.NOT_FOUND.value(), "NOT_FOUND",
                        ex.getMessage(), correlationId(request)));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(HttpStatus.FORBIDDEN.value(), "FORBIDDEN",
                        "You do not have access to this resource", correlationId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST",
                        ex.getMessage(), correlationId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                        message, correlationId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex,
                                                     HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                        ex.getMessage(), correlationId(request)));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
