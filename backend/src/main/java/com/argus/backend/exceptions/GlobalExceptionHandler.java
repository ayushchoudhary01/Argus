package com.argus.backend.exceptions;

import com.argus.backend.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Without this, an unhandled {@link PipelineException} produced Spring's stock error
 * body: a timestamp, a path, and no indication of what failed or which run it belonged
 * to. The correlation id was generated, threaded through every log line, and then
 * discarded at the boundary.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PipelineException.class)
    public ResponseEntity<ErrorResponse> handlePipelineFailure(PipelineException e) {
        log.error("Pipeline failed [correlationId={}] {}", e.getCorrelationId(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("PIPELINE_FAILED", e.getMessage(), e.getCorrelationId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException e) {
        List<String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.warn("Request rejected by validation: {}", fieldErrors);
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.validation("Request failed validation", fieldErrors));
    }

    /**
     * Anything that reaches here is a bug rather than an expected pipeline outcome, so
     * the stack trace is logged in full while the caller gets a deliberately generic
     * message — internal details stay out of the response body.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred", null));
    }
}
