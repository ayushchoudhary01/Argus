# 004 — Why No LLM Confidence Scores

## Status
Accepted

## Context
A natural feature request for a multi-agent reasoning system is to ask each agent to output a confidence score alongside its thesis — e.g. "I am 87% confident in this analysis."

## Decision
LLM-generated confidence scores are not used anywhere in AMRS.

## Reasoning
LLM confidence scores are not calibrated signals. They are prompt-influenced outputs. Instruct a model to be aggressive and it outputs 0.9. Instruct it to be conservative and it outputs 0.4. The number reflects the prompt, not the underlying epistemic state of the model.

In a financial reasoning context, a fabricated confidence score is worse than no score at all. It creates an illusion of precision that could influence interpretation in the wrong direction.

Conflict detection in AMRS is non-generative — deterministic causal-chain comparison plus a DeBERTa NLI classifier. The Contrarian Agent does emit a categorical assessment (SUBSTANTIVE / LOW_VARIANCE / DATA_GAP), but it is never the final answer: the rules engine assigns the 4-type label in code, and only accepts that verdict when the independently detected conflict evidence agrees with it. What the system refuses is a number from an LLM, not a judgement.

## Consequences
- No false precision in the final report
- Divergence classification is fully auditable
- Users interpret agent reasoning directly rather than anchoring on a fabricated number