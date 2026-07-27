package com.alahly.momkn.finthos.common.error;
import lombok.Getter;

@Getter
public class ProcessorUnavailableException extends RuntimeException {

    private final String reference;

    public ProcessorUnavailableException(String reference) {
        super("Processor unavailable for transaction: " + reference);
        this.reference = reference;
    }

    public ProcessorUnavailableException(String reference, Throwable cause) {
        super("Processor unavailable for transaction: " + reference, cause);
        this.reference = reference;
    }

}
