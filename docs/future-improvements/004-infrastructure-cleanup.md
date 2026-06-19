# Infrastructure Cleanup

## Flyway Auto-Run Issue
Flyway doesn't auto-run on startup in Spring Boot 4.1.x as currently configured.
Working around it by manually applying `V1__create_schema.sql` via psql before
first run. Needs a proper fix — likely a Flyway/Spring Boot 4.x compatibility
configuration issue, not yet root-caused.

## Spring Data Redis Repository Scan Warning
Spring Data Redis scans for repositories on startup and warns about
`DivergenceReportRepository` (a JPA repository) not being a Redis repository.
Resolves correctly (0 Redis repos found) but the warning is noise. Fix: explicit
`@EnableJpaRepositories` to scope JPA scanning and silence the Redis scan entirely.

## Redis Cache Verification
Caching by event hash is implemented but not explicitly verified — need to confirm
that submitting the identical event twice returns the cached result without
re-running the full agent pipeline.

## Ollama Cold-Start Latency
First request after container start may be slower if Ollama hasn't pre-loaded the
model into memory. Worth investigating a warm-up call on backend startup.

## Status
None of these block the demo. All are known, minor, and documented here so they
don't get forgotten.