# 003 — Python Sidecar Called Twice

## Status
Accepted

## Context
The Python sidecar handles two distinct responsibilities: deterministic market analytics (Layer 0) and NLI-based contradiction detection (Layer 2). A design decision was needed on whether these should be a single call or two separate calls.

## Decision
Spring Boot calls the Python sidecar twice — once before agent execution and once after.

## Reasoning
The two responsibilities are fundamentally different in nature and timing.

Layer 0 analytics must run before agents execute — agents need the MarketContext to reason. Layer 2 contradiction detection must run after all three agents have produced outputs — it has nothing to work with until then.

Collapsing both into a single call would require Python to own agent orchestration, which violates the principle established in ADR-002. Spring Boot would become a dumb proxy, losing control over contract validation, retries, caching, and the CompletableFuture barrier between agents.

The two-hop cost is a deliberate tradeoff: two clean, bounded REST calls with clear contracts, in exchange for keeping orchestration ownership where it belongs.

## Consequences
- Two network round-trips per pipeline execution — latency cost is accepted
- Each Python endpoint has a single, well-defined responsibility
- Spring Boot retains full control over pipeline sequencing