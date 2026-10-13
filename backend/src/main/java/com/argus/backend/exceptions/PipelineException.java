package com.argus.backend.exceptions;

public class PipelineException extends RuntimeException {

    /**
     * Used by clients that fail before the run's correlation id is in scope.
     * {@code OrchestrationService} replaces it with the real id on the way out.
     */
    public static final String UNKNOWN = "unknown";

    private final String correlationId;

    public PipelineException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
