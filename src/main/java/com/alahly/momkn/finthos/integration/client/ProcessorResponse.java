package com.alahly.momkn.finthos.integration.client;

public record ProcessorResponse(String reference, String status, String authCode) {

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isDeclined() {
        return "DECLINED".equals(status);
    }

    public boolean isTimeout() {
        return "TIMEOUT".equals(status);
    }
}
