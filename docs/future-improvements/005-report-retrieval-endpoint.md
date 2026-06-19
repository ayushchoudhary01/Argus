# Report Retrieval Endpoint

## Current State
`DivergenceReport` objects are persisted to PostgreSQL asynchronously after every
pipeline run. `DivergenceReportRepository.findByCorrelationId()` already exists and is
ready to use — but nothing in the codebase calls it. There is exactly one endpoint,
`POST /api/v1/analysis`, and no corresponding `GET`. Persistence is currently write-only.

## Idea
Add `GET /api/v1/analysis/{correlationId}` to retrieve a previously generated report.
Each analysis response already includes enough context that a correlationId could be
surfaced to the frontend and used to revisit past results without re-running the pipeline.

## Why It's Deferred, Not a Bug
The core demo value — submit an event, get a divergence analysis — doesn't depend on
retrieval. This is a genuinely separate feature: building a history view, a way to browse
or search past analyses, possibly pagination. It's scoped as an addition, not a fix.

## What It Would Take
- New `@GetMapping` route on `AnalysisController`
- Decide what's returned if the correlationId doesn't exist — 404 vs empty response
- Frontend: surface correlationId after analysis, add a way to revisit past reports
  (history list, URL-based deep link, etc.)

## Status
Not started. Known, intentional gap — documented here so it's a deliberate decision
to explain if asked, not an oversight to be caught off guard by.