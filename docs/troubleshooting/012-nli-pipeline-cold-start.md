# NLI Pipeline Lazy-Loaded on First Request Instead of Startup

## Problem
The DeBERTa NLI model weights were pre-cached into the Docker image at build time (see
ADR/troubleshooting on Docker pre-caching), but the `pipeline()` object itself — loading
those weights into memory — still only initialized on the first `/nli` request, not at
container startup. This meant the very first real user request after a fresh container
start was noticeably slower than every subsequent one.

## Root Cause
```python
_nli_pipeline = None

def get_nli_pipeline():
    global _nli_pipeline
    if _nli_pipeline is None:
        _nli_pipeline = pipeline("text-classification", model=NLI_MODEL, device=-1)
    return _nli_pipeline
```

Lazy initialization is a reasonable default in general, but for a demo system where the
first request is often the one happening live in front of an interviewer, "first request
is slow" is the worst possible time for that cost to surface.

## Fix
Called `get_nli_pipeline()` once during the FastAPI `lifespan` startup handler in
`main.py`, so model weights are loaded into memory before the server starts accepting
requests:

```python
logger.info("Warming up NLI pipeline...")
get_nli_pipeline()
logger.info("NLI pipeline warmed and ready")
```

## Rule
For any model or expensive resource that's reused across requests, prefer eager
initialization at application startup over lazy initialization on first use — especially
in systems where the first request matters disproportionately (demos, low-traffic
services, anything judged by first impression).