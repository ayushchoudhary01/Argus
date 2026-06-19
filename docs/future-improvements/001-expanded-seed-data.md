# Expanded Seed Data

## Current State
24 historical events, 1977–2023, covering major macro regimes — stagflation, rate
shock cycles, financial crises, geopolitical shocks.

## Idea
Expand coverage with more granular events: individual FOMC decisions, earnings season
surprises, sector-specific shocks, non-US macro events (ECB, BOJ policy moves).

## Why It Matters
Vector similarity search is only as good as what it has to compare against. More
events — especially ones covering scenarios not yet represented — would improve
historical analogue quality, particularly for events that don't cleanly map to the
existing 24.

## What's Actually Hard
Not the data collection — the `what_happened` outcome descriptions need to be
accurate and well-written, since agents reason over them directly. Garbage outcome
descriptions would actively hurt agent reasoning quality, not just add noise.

## Status
Not started. Quality over quantity — even 10 more well-researched events would beat
50 shallow ones.