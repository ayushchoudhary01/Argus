# 009 — Why NLI Over LLM for Contradiction Detection

## Status
Accepted

## Context
Contradiction detection between agent assumptions requires comparing pairs of statements and determining whether they are in conflict. Two approaches were considered: using a generative LLM (e.g. a fast 8B model) prompted to identify contradictions, or using a dedicated NLI classification model (DeBERTa-v3-base).

## Decision
DeBERTa-v3-base NLI is used for Stage B contradiction detection. A generative LLM is not used for this task.

## Reasoning
Contradiction detection is a classification problem, not a generation problem. Given a premise and a hypothesis, the required output is one of three labels — ENTAILMENT, CONTRADICTION, or NEUTRAL — plus a probability score. This is precisely what NLI models are designed and trained for.

Using a generative LLM for this task introduces several problems:

**Non-determinism.** A generative model produces free-text reasoning before arriving at a label. The same pair of statements can produce different outputs across runs. A classification model produces a stable probability distribution every time.

**No calibrated probability.** A generative model cannot produce a meaningful probability score for its contradiction assessment. DeBERTa outputs a softmax probability per class — this is what enables the configurable threshold (0.85) and makes every flagged item auditable with a concrete score.

**Latency and compute.** Running an 8B generative model per assumption pair adds significant inference time. DeBERTa-v3-base runs on CPU in approximately 50ms per pair at 500MB RAM. For a task that requires comparing multiple assumption pairs per pipeline execution, this difference compounds.

**Prompt sensitivity.** A generative LLM's contradiction assessment can shift based on prompt phrasing, temperature, and context window content. The assessment would reflect prompt engineering as much as actual logical tension — which is the same problem that disqualified LLM confidence scores in ADR-004.

The known limitation — that DeBERTa detects linguistic tension rather than economic contradiction — is handled honestly through labelling (`FLAGGED_TENSION`) and dashboard presentation, as documented in ADR-006.

## Consequences
- Contradiction detection is deterministic, auditable, and reproducible
- Every flagged pair carries a concrete probability score
- CPU-only inference at low latency is viable for the pipeline's throughput requirements
- The system does not overclaim economic contradiction from a generative model's opinion