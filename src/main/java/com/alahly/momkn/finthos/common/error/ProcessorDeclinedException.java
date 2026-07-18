package com.alahly.momkn.finthos.common.error;

public class ProcessorDeclinedException extends RuntimeException {

    private final String reference;

    public ProcessorDeclinedException(String reference) {
        super("Processor declined transaction: " + reference);
        this.reference = reference;
    }

    public String getReference() {
        return reference;
    }
}
