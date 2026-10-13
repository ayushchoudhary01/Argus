# Infrastructure Cleanup

## RESOLVED — Flyway Auto-Run Issue
Flyway never ran, on any boot, and never logged an error.

Root cause: Spring Boot 4 split autoconfiguration into per-technology modules.
`org.flywaydb:flyway-core` supplies the engine, but nothing reads `spring.flyway.*`
unless `org.springframework.boot:spring-boot-starter-flyway` is on the classpath.
With no autoconfiguration present there was nothing to emit a warning, so the
failure was completely silent — which is why it read as "works after a manual run":
the manual `psql` step was creating the schema, and the app then ran happily against
whatever tables existed, with `ddl-auto: none`.

Confirmed by checking that `FlywayAutoConfiguration` was absent from
`spring-boot-autoconfigure-4.1.0.jar`, and that no `flyway_schema_history` table had
ever been created. Adding the starter fixed it. See ADR 010.

## RESOLVED — Redis Cache Verification
Verified. Submitting an identical event (same normalised text, same asset set)
returns the cached report immediately instead of re-running the agents — confirmed
end-to-end through the dashboard against a run that had taken ~160s cold.

## Spring Data Redis Repository Scan Warning
Spring Data Redis scans for repositories on startup and warns about
`DivergenceReportRepository` (a JPA repository) not being a Redis repository.
Resolves correctly (0 Redis repos found) but the warning is noise. Fix: explicit
`@EnableJpaRepositories` to scope JPA scanning and silence the Redis scan entirely.

## Ollama Cold-Start Latency
Confirmed real and measurable: the first analysis after container start took ~160s,
a subsequent identical-shape run took ~51s, with the difference being model load
rather than reasoning. Worth a warm-up call on backend startup, as the sidecar
already does for the NLI pipeline.

## Analytics Layer Has No Data-Quality Signal
The historical layer labels missing data — `event_outcomes.quality` is `VERIFIED` or
`UNAVAILABLE`, and `historical_coverage` flags insufficient retrieval. The live
analytics layer has no equivalent. Every service catches its own exceptions and
returns an empty context, so a failed yfinance fetch reaches the agent prompt as a
bare `null` with nothing marking it as missing rather than genuinely absent.

`get_anomaly_flags` was the worst case — an empty list meant both "no anomalies" and
"could not check" — and now appends `anomaly_check_unavailable` on failure. The same
treatment should extend to volatility, macro and technical: a `DataQuality` field on
each context block, surfaced in the report the way `historical_coverage` already is.

Relevant because yfinance is unofficial and rate-limits without warning.

## Sidecar Endpoints Block the Event Loop
`/analytics` and `/nli` are declared `async def` but call blocking work directly —
yfinance HTTP, psycopg2, and torch inference. That occupies the event loop for the
duration, so concurrent requests queue behind each other instead of interleaving.

Harmless at the current scale (local, one analysis at a time, and the pipeline is
deliberately sequential) but wrong as written. The fix is to declare both handlers
`def` rather than `async def`, letting FastAPI run them in its threadpool.

## `asset_context` Does Not Influence Analysis
The API accepts `asset_context` and the dashboard collects it, but it reaches nothing
except the Redis cache key. It is never interpolated into an agent prompt, never
narrows the analytics layer (which is hardcoded to SPY / ^VIX / ^TNX / ^IRX / DXY), and
never filters historical retrieval. Submitting the same event with different assets
produces a different cache entry and an identical analysis.

Either wire it through — scope the analytics tickers and name the assets in the prompts —
or drop it from the request contract. Carrying a validated field that changes nothing is
the worse of the three options.

## Status
None of these block the demo. All are known, minor, and documented here so they
don't get forgotten.
