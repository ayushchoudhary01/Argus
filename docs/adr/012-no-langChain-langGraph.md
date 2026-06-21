# ADR 012: No LangChain / LangGraph

## Status
Accepted

## Context
LangChain and LangGraph are widely-used Python frameworks for building LLM-powered
applications and multi-agent orchestration specifically. LangGraph in particular models
a pipeline as a graph of nodes and edges — closely matching Argus's shape: multiple
agents, a defined flow, conditional routing based on outputs (the divergence
classification rules engine).

## Decision
Argus's orchestration layer is built natively in Spring Boot — `OrchestrationService`
manages the CompletableFuture barrier for the Agent 1/Agent 2 fan-out, virtual threads
for concurrency, and a typed pipeline from request to DivergenceReport. No orchestration
framework is used.

## Reasoning
LangChain/LangGraph are Python-first. Adopting either would mean the orchestration layer
— currently Spring Boot — becomes Python, since these frameworks aren't naturally suited
to being called from Java in a way that preserves their value (graph definition, state
management, conditional edges).

This project exists specifically to demonstrate enterprise Java backend capability
alongside AI integration — not to demonstrate Python AI tooling fluency. A Python-based
orchestrator would have been faster to build in some respects, but it would have replaced
the strongest signal this project is meant to send: that Java/Spring Boot can own a
complex, stateful, concurrent AI pipeline end-to-end with full type safety and lifecycle
auditability.

## Consequences
- More orchestration code written by hand (the CompletableFuture barrier, virtual thread
  executor, manual fan-out/fan-in) than a graph framework would have required
- Full control over concurrency model, typing, and lifecycle — every step is auditable
  in one Java codebase rather than split across a framework's internals
- If this system needed to scale to many more agents or a much more complex graph of
  conditional steps, a framework like LangGraph would start to look more attractive —
  Argus's current shape (3 fixed agents, 1 barrier, 1 sequential step) is simple enough
  that hand-rolled orchestration is still the right level of abstraction

## Alternatives Considered
- **LangGraph** — rejected; Python-first, would have meant moving the entire
  orchestration layer out of Spring Boot
- **LangChain** (without LangGraph) — rejected for the same reason, and additionally
  LangChain's abstractions (chains, output parsers) solve problems this project already
  solves more explicitly with typed DTOs and Jackson annotations