# Jackson Snake Case Deserialization Mismatch

## Problem
Java fields using camelCase (e.g. `reasoningChain`) fail to deserialize from JSON responses
using snake_case (e.g. `reasoning_chain`). Jackson silently sets the field to null instead
of throwing — so the error surfaces downstream as a NullPointerException or validation
failure, not a deserialization error.

## Symptoms
- `MethodArgumentNotValidException` with rejected value null on a field you know was sent
- `AgentOutputValidator` reporting missing reasoning chain despite valid Ollama response
- `NullPointerException` on a getter call for a field that appears populated in logs

## Root Cause
Jackson defaults to camelCase field name matching. Any field name mismatch between the
Java model and the JSON wire format results in silent null assignment.

## Fix
Add `@JsonProperty("snake_case_name")` explicitly on every Java field whose wire format
name differs from its Java name.

```java
@JsonProperty("reasoning_chain")
private List<String> reasoningChain;
```

## Affected Files in AMRS (as of fix)
- `AgentOutput.java` — reasoning_chain, causal_chain, explicit_assumptions, historical_refs,
  key_uncertainties, divergence_assessment, challenged_assumptions, validated_assumptions
- `CausalChain.java` — target_variable
- `NLIRequest.java` — causal_chains
- `EventRequest.java` — asset_context
- `MarketContext.java` — anomaly_flags, historical_coverage, historical_similarity
- `TechnicalContext.java` — spy_rsi_14, spy_trend
- `VolatilityContext.java` — vix_level, percentile_30d
- `MacroContext.java` — yield_curve_slope, dxy_momentum
- `HistoricalAnalogue.java` — event_id, similarity_score, what_happened
- `EventOutcome.java` — window_days, return_pct
- `ContradictionMap.java` — directional_conflicts, flagged_tensions, neutral_pairs
- `DirectionalConflict.java` — target_variable
- `DivergenceReport.java` — divergence_type, primary_thesis, strongest_counter_thesis,
  key_conflicts, historical_analogues, historical_coverage

## Rule
Any Java class that participates in JSON serialization or deserialization — whether
receiving from Ollama, the Python sidecar, or sending to the frontend — must have
explicit `@JsonProperty` on every field whose name differs from its wire format name.
Internal-only classes (e.g. `AgentContext`) are exempt.