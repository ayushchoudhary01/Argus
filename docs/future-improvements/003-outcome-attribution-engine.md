# Milestone 2 — Outcome Attribution Engine

## Current State
Not built. This was always planned as the second phase, after the core reasoning
pipeline was proven to work end-to-end (it now does).

## Idea
A scheduled background job runs at 30, 60, and 90 days after each analysed event.
It fetches the actual market outcome and retrospectively scores each agent:
- Was the agent's thesis directionally correct?
- Which explicit assumptions held, which failed?
- Did the historical analogue's outcome pattern actually repeat?
- How often does the Contrarian Agent catch genuine reversals vs. false positives?

## Why It Matters
This is what turns Argus from a reasoning machine into a learning system. Right now,
agent output quality is judged only by internal coherence (does it pass the output
contract). This milestone adds an external, ground-truth check — actual market
behaviour — against agent theses.

Concretely: over enough events, you'd learn things like "the Structural Agent is
directionally correct 68% of the time" or "the 1977 analogue has 0.84 vector
similarity but only 0.52 outcome-prediction accuracy" — real calibration data, not
guesses.

## What's Needed
- Scheduled job (Spring `@Scheduled` or similar)
- New PostgreSQL table for retrospective scoring records
- Agent Performance Panel in the dashboard to display this over time

## Status
Designed, not built. Natural next phase once the demo is stable.