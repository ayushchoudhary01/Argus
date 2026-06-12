# 007 — Agent 3 Escape Hatch: LOW_VARIANCE

## Status
Accepted

## Context
Agent 3 (Contrarian Agent) is mandated to challenge the reasoning of Agents 1 and 2. A design decision was needed on whether Agent 3 must always produce a contrarian thesis.

## Decision
Agent 3 is explicitly permitted — and instructed — to output `LOW_VARIANCE` when the combined reasoning of Agents 1 and 2 is internally consistent and well-supported by the data. Disagreement is not mandatory.

## Reasoning
Forcing an agent to always disagree produces hallucinated disagreement. If the consensus is correct, a mandatory contrarian will fabricate gaps that do not exist. That output is noise, not signal — and in a financial reasoning context, noise is dangerous.

`LOW_VARIANCE` is a meaningful, high-value output. It means three agents with genuinely different mandates, different tool access, and different reasoning lenses attempted to find contradictions and could not. That is a strong conviction signal — arguably more valuable than a fabricated disagreement.

The system prompt for Agent 3 explicitly states: "Inventing disagreement where none exists is a reasoning failure, not a success."

## Consequences
- Agent 3 output is trustworthy in both directions — SUBSTANTIVE and LOW_VARIANCE carry real signal
- The rules engine downstream can produce CONSENSUS_HIGH_CONVICTION as a valid, meaningful divergence type
- Hallucinated contrarianism is structurally prevented at the prompt level