package com.alahly.momkn.finthos.common.error;

public record ApiError(int status, String code, String message) {
}
