package com.alahly.momkn.finthos.common.error;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message, String correlationId) {

    public ApiError(int status, String error, String message, String correlationId) {
        this(Instant.now(), status, error, message, correlationId);
    }
}
