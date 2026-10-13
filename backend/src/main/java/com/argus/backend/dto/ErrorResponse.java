package com.argus.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Error body returned to API callers.
 *
 * <p>The correlation id is the point of this type. Every pipeline run generates one and
 * logs it at each step, but until now it never left the process, so a caller holding a
 * 500 had no way to tie it back to the logs or to the persisted report. Returning it
 * makes a failed request traceable without shell access to the container.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,

        @JsonProperty("correlation_id")
        String correlationId,

        @JsonProperty("field_errors")
        List<String> fieldErrors,

        Instant timestamp
) {
    public static ErrorResponse of(String error, String message, String correlationId) {
        return new ErrorResponse(error, message, correlationId, null, Instant.now());
    }

    public static ErrorResponse validation(String message, List<String> fieldErrors) {
        return new ErrorResponse("VALIDATION_FAILED", message, null, fieldErrors, Instant.now());
    }
}
