# 002 — Spring Boot Owns the Lifecycle

## Status
Accepted

## Context
AMRS involves multiple services: a Python sidecar, Ollama for LLM inference, Qdrant for vector search, PostgreSQL, and Redis. A decision was needed on where orchestration responsibility lives.

## Decision
Spring Boot is the sole orchestrator. Every action in the pipeline — calling Python, calling Ollama, enforcing contracts, caching, storing results — is initiated and controlled by Spring Boot. No other service initiates anything independently.

## Reasoning
Distributed orchestration — where multiple services share control — creates debugging complexity, unclear failure ownership, and unpredictable retry behaviour. In a BFSI context, auditability and control flow are non-negotiable.

Centralising orchestration in Spring Boot means there is always one place to trace what happened, in what order, and why. Every step is a deliberate, auditable action by a single owner.

Python and Ollama are workers. They respond to Spring Boot's calls and return results. They do not make decisions about pipeline flow.

## Consequences
- Full pipeline traceability from a single service
- Failure handling, retries, and fallbacks are managed in one place
- Spring Boot becomes a critical path dependency — its failure stops the pipeline entirely, which is acceptable given it is the orchestrator by design