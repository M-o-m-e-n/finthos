package com.alahly.momkn.finthos.common.error;

public class IdempotencyConflictException extends RuntimeException {

    public enum Reason {
        KEY_REUSED_WITH_DIFFERENT_REQUEST,
        REQUEST_IN_PROGRESS,
        PREVIOUS_ATTEMPT_FAILED
    }

    private final Reason reason;

    public IdempotencyConflictException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
