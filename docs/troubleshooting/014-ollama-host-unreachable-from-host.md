# Seeding Silently Produces Nothing: OLLAMA_HOST Is Container-Scoped

## Symptom
`python sidecar/seed/seed.py` runs to completion and exits 0. PostgreSQL fills with 24
events and 345 outcome rows. Qdrant stays empty, so every analysis afterwards returns
`historical_coverage: INSUFFICIENT` and the dashboard shows "Low Historical Confidence"
no matter what event is submitted.

## Root Cause
`.env` sets:

```
OLLAMA_HOST=http://host.docker.internal:11434
```

That value is correct for the **sidecar container**, which has to reach Ollama running
on the host, and `host.docker.internal` is the Docker Desktop name for exactly that.

But `seed.py` runs **on the host**, not in a container, and reads the same variable.
`host.docker.internal` does not resolve from the host:

```
curl -s -o /dev/null -w "%{http_code}" http://host.docker.internal:11434/api/tags
000
```

So every `ollama.embeddings(...)` call fails. The failure is per-event and caught by the
`try/except` around `seed_event`, which logs it and appends to `failed` — but the
PostgreSQL inserts for that event happened *before* the embedding call, so the
relational half of the seed succeeds while the vector half never runs.

One `.env` variable with two different correct values depending on where the process
runs, and a partial-failure path that leaves the two stores inconsistent.

## Fix
Override it for the host-side seed run:

```bash
cd sidecar
OLLAMA_HOST=http://localhost:11434 python seed/seed.py
```

Now documented in the README's seeding section, which previously did not mention the
seed step at all.

## Verification
```
events: 24   outcomes: 345   VERIFIED: 316   UNAVAILABLE: 29
qdrant points: 24
```

The 29 `UNAVAILABLE` rows are expected and correct: `GC=F` has no Yahoo Finance history
before roughly August 2000, which covers the eight seeded events from 1977 to March 2000.
They are labelled rather than imputed.

## Rules

**A single env var cannot serve both a container and a host process when its value is a
container-scoped hostname.** Either give the host-side tooling its own variable, or have
`seed.py` fall back to `localhost` when `host.docker.internal` does not resolve.

**Partial success across two datastores is worse than outright failure.** Seeding writes
PostgreSQL first and Qdrant second; a failure between them leaves a database that looks
populated and a vector store that is empty, which then presents as a vague retrieval
quality problem rather than a seeding error. Writing both or neither per event, or
verifying point count at the end of the run, would have surfaced it immediately.
