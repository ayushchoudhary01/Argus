# 008 — Causal Chain Directional Check Before NLI

## Status
Accepted

## Context
Contradiction detection between agent outputs could be handled entirely by DeBERTa NLI. A design decision was needed on whether a pre-NLI deterministic check was warranted.

## Decision
A causal chain directional check runs before DeBERTa. Any agent pair sharing the same `target_variable` but with opposing `direction` values (`UPWARD_PRESSURE` vs `DOWNWARD_PRESSURE`) is immediately flagged as `DIRECTIONAL_CONFLICT` and skipped in the NLI stage.

## Reasoning
Financial language is imprecise and paraphrased differently across agents. NLI may miss a direct directional conflict if the phrasing diverges enough — "the Fed will tighten further" and "rate cuts are coming" are semantically opposite but phrased in ways that a general-purpose NLI model may not reliably catch.

The causal chain fields (`driver`, `direction`, `target_variable`) are structured enums, not free text. Comparing them is a deterministic string match — fast, reliable, and requires no model. This catches the most critical class of macro conflicts with certainty before probabilistic NLI runs.

Pairs already caught deterministically are excluded from Stage B, avoiding redundant inference compute.

## Consequences
- The most important class of conflicts (opposing directional pressure on the same variable) is caught with 100% reliability
- NLI compute is reserved for assumption pairs where deterministic checks are insufficient
- Detection method is transparent and auditable per flagged item in the ContradictionMap