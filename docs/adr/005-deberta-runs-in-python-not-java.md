# 005 — DeBERTa Runs in Python, Not Java

## Status
Accepted

## Context
DeBERTa-v3-base is a PyTorch model used for NLI-based contradiction detection. A decision was needed on where this model runs — inside the Java runtime or in the Python sidecar.

## Decision
DeBERTa runs exclusively inside the Python sidecar. It is never loaded into the Java runtime.

## Reasoning
Running PyTorch models inside a JVM requires Deep Java Library (DJL) or similar native wrappers. These introduce native dependency management complexity, platform-specific build issues, and unstable behaviour in production environments — all unacceptable in a BFSI-grade system.

Python is the native runtime for PyTorch. The model loads cleanly, inference is straightforward, and the dependency chain is standard and well-understood. There is no engineering benefit to forcing a PyTorch model into Java when a Python sidecar already exists for exactly this purpose.

The sidecar runs DeBERTa on CPU. At approximately 500MB RAM and ~50ms per inference pair, the performance profile is acceptable for this pipeline.

## Consequences
- Clean, stable model loading with no JVM native wrapper risk
- DeBERTa is fully isolated — updates or swaps require no changes to Spring Boot
- CPU-only inference is accepted given the low inference volume per pipeline execution