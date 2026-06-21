# Persistence Retry and Monitoring

## Current State
`PersistenceService.persist()` runs asynchronously on a dedicated virtual thread executor
(fixed from the original shared-common-pool implementation — see troubleshooting docs).
It catches its own exceptions and logs them, so failures aren't completely silent.
However, there is still no retry on transient failure, no dead-letter handling, and no
metric/alert if persistence starts failing repeatedly.

## Idea
- Retry with backoff on transient PostgreSQL connection failures
- A dead-letter mechanism — if a report repeatedly fails to persist, write it somewhere
  recoverable (a local file, a Redis list) rather than losing it after logging
- A metric (e.g. via Micrometer/Actuator) tracking persistence success/failure rate, so a
  silent degradation in the database layer would actually surface as a visible signal

## Why It's Deferred
The dedicated executor fix already addresses the most pressing concern (no longer
competing with other async work on the shared pool). Full retry/monitoring is a genuine
production-readiness feature, not a correctness bug — the current behavior is "log and
move on," which is acceptable for a local demo system where the user can see the logs
directly.

## Status
Not started. Documented as a known, deliberate gap.