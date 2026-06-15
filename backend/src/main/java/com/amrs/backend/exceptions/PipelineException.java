package com.amrs.backend.exceptions;

public class PipelineException extends RuntimeException {

    private final String correlationId;

    public PipelineException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
