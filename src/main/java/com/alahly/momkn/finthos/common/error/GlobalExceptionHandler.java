package com.alahly.momkn.finthos.common.error;

import com.alahly.momkn.finthos.common.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(ProcessorUnavailableException.class)
    public ResponseEntity<ApiError> handleProcessorUnavailable(ProcessorUnavailableException ex,
                                                               HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(HttpStatus.SERVICE_UNAVAILABLE.value(), "PROCESSOR_UNAVAILABLE",
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

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotencyConflict(IdempotencyConflictException ex,
                                                              HttpServletRequest request) {
        HttpStatus status = ex.getReason() == IdempotencyConflictException.Reason.KEY_REUSED_WITH_DIFFERENT_REQUEST
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.CONFLICT;
        return error(status, "IDEMPOTENCY_" + ex.getReason().name(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransactionStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidTransactionStateException ex,
                                                       HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                         HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The wallet was modified concurrently; please retry", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        log.warn("Data integrity violation", ex);
        return error(HttpStatus.CONFLICT, "CONFLICT",
                "The request conflicts with the current state of the resource", request);
    }

    /** Spring Data JDBC wraps persistence failures; dispatch on the real cause. */
    @ExceptionHandler(DbActionExecutionException.class)
    public ResponseEntity<ApiError> handleDbAction(DbActionExecutionException ex,
                                                   HttpServletRequest request) {
        if (ex.getCause() instanceof OptimisticLockingFailureException cause) {
            return handleOptimisticLock(cause, request);
        }
        if (ex.getCause() instanceof DataIntegrityViolationException cause) {
            return handleDataIntegrity(cause, request);
        }
        return handleUnexpected(ex, request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex,
                                                        HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MISSING_HEADER",
                "Required header '" + ex.getHeaderName() + "' is missing", request);
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
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), code, message, correlationId(request)));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}
