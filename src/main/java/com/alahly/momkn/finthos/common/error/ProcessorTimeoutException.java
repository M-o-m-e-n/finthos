package com.alahly.momkn.finthos.common.error;

public class ProcessorTimeoutException extends RuntimeException {

    private final String reference;

    public ProcessorTimeoutException(String reference) {
        super("Processor timed out for transaction: " + reference);
        this.reference = reference;
    }

    public ProcessorTimeoutException(String reference, Throwable cause) {
        super("Processor timed out for transaction: " + reference, cause);
        this.reference = reference;
    }

    public String getReference() {
        return reference;
    }
}
