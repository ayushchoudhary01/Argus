# Connection Failures Bypass RestClient's onStatus Handler

## Problem
A `@RestControllerAdvice` was added so that a failed pipeline run returns its correlation
id to the caller instead of Spring's stock error body. It compiled, the unit tests passed,
and the validation path worked exactly as intended:

```
HTTP 400  {"error":"VALIDATION_FAILED","field_errors":["event: size must be between 10 and 2000"]}
```

But stopping the sidecar and submitting a real event produced the *generic* handler, not
the pipeline one — no correlation id, no useful message:

```
HTTP 500  {"error":"INTERNAL_ERROR","message":"An unexpected error occurred"}
```

## Root Cause
Both clients translated sidecar and Ollama failures into `PipelineException` like this:

```java
.retrieve()
.onStatus(HttpStatusCode::isError, (req, res) -> {
    throw new PipelineException("Sidecar /analytics failed with status: " + res.getStatusCode(), ...);
})
```

`onStatus` is a **response** predicate. It can only run once there is a response with a
status code to inspect. When the host is unreachable — container stopped, wrong port, DNS
failure — `RestClient` throws `ResourceAccessException` during the exchange itself. No
response is ever produced, so the predicate never evaluates and the exception propagates
as a raw `RestClientException`.

The result: HTTP error codes from the sidecar were wrapped correctly, and connection
failures were not wrapped at all — despite a refused connection being by far the more
likely failure in a local Docker stack, and the exact case the correlation id was added
to help diagnose.

## Fix
Wrap the exchange itself, in addition to the status predicate, in both
`PythonSidecarClient` (two call sites) and `OllamaClient`:

```java
try {
    context = restClient.post()
            ...
            .onStatus(HttpStatusCode::isError, (req, res) -> { throw new PipelineException(...); })
            .body(MarketContext.class);
} catch (RestClientException e) {
    // onStatus only fires when a status exists; a refused connection never gets one.
    throw new PipelineException("Sidecar /analytics unreachable: " + e.getMessage(),
            PipelineException.UNKNOWN);
}
```

`PipelineException` thrown by `onStatus` is not a `RestClientException`, so it passes
through the catch untouched rather than being re-wrapped.

## Verification
With the sidecar stopped:

```
HTTP 500  {"error":"PIPELINE_FAILED",
           "message":"Sidecar /analytics unreachable: I/O error on POST request ...",
           "correlation_id":"5c1914cc-9650-4598-aa69-2348de2830b8"}
```

And that id resolves in the logs, which was the whole point:

```
INFO  OrchestrationService    : Pipeline start [correlationId=5c1914cc-...] event='...'
ERROR GlobalExceptionHandler  : Pipeline failed [correlationId=5c1914cc-...] Sidecar /analytics unreachable
```

## Rules

**`onStatus` covers protocol errors, not transport errors.** Anything that prevents a
response from existing at all — connection refused, timeout, DNS failure, TLS handshake
failure — needs a `try/catch` around the exchange. A client that only handles 4xx/5xx is
handling the half of the failure space that requires the remote service to be up.

**Testing a fix is not the same as testing the failure it fixes.** The error handler was
verified against a validation error, which worked, and that was nearly accepted as proof
the whole thing worked. The bug surfaced only after deliberately stopping a dependency.
Exercise the failure you built the handler for, not the nearest convenient one.
