# 010 — Flyway for Database Migrations

## Status
Accepted

## Context
AMRS requires a PostgreSQL schema and seed data to be present before the system can run. A decision was needed on how to manage schema initialisation and versioning across environments and repository clones.

## Decision
Flyway Community Edition manages all database migrations. Versioned SQL files live in `orchestrator/src/main/resources/db/migration/` and are applied automatically on Spring Boot startup.

## Reasoning
Manual schema setup is error-prone and undocumented. A developer cloning the repository should not need to run separate scripts or remember setup steps — the application itself should bring the database to the correct state on first boot.

Flyway provides versioned, auditable, repeatable migrations. Every schema change is a numbered file with a clear history. This is standard practice in BFSI environments where schema changes must be traceable.

The seed script remains as an internal tool for generating verified outcome data from yfinance. Its output is exported into a Flyway migration file, making the seed data part of the versioned migration history rather than a runtime dependency.

## Consequences
- Any developer cloning the repo gets a fully initialised database on first Spring Boot startup
- Schema changes are versioned and auditable
- The seed script is a one-time internal tool, not a user-facing setup step