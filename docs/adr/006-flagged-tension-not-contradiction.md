# 006 — FLAGGED_TENSION, Not CONTRADICTION

## Status
Accepted

## Context
DeBERTa-v3-base is used to detect tension between agent assumptions. The question was how to label outputs where DeBERTa returns a high-probability CONTRADICTION classification.

## Decision
DeBERTa outputs that exceed the probability threshold are labelled `FLAGGED_TENSION` in the final report, not `CONTRADICTION`.

## Reasoning
DeBERTa is trained on general corpora. It detects linguistic tension — sentences that are structurally in conflict in natural language. It does not understand economic transmission mechanisms.

In macroeconomics, statements that appear linguistically contradictory can coexist perfectly. "Consumer spending is resilient" and "corporate profit margins contract sharply" are linguistically tense but economically compatible — many industries face margin compression while consumer demand holds. DeBERTa would flag this as a contradiction. It is not one.

Labelling these as `FLAGGED_TENSION` is honest. It tells the user: a linguistic tension was detected here, and economic interpretation is required. It does not overclaim. The system surfaces the signal and defers judgment to the analyst.

## Consequences
- The system does not fabricate economic contradictions from linguistic patterns
- Users are prompted to interpret flagged pairs rather than accept them as definitive conflicts
- The label distinction (`DIRECTIONAL_CONFLICT` vs `FLAGGED_TENSION`) communicates detection method clearly