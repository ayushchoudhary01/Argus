# Report Retrieval Endpoint

## Current State
`DivergenceReport` objects are persisted to PostgreSQL asynchronously after every
pipeline run. `DivergenceReportRepository.findByCorrelationId()` already exists and is
ready to use — but nothing in the codebase calls it. There is exactly one endpoint,
`POST /api/v1/analysis`, and no corresponding `GET`. Persistence is currently write-only.

## Idea
Add `GET /api/v1/analysis/{correlationId}` to retrieve a previously generated report.

The prerequisite is now in place: `DivergenceReport` returns `correlation_id`, and
failures return the same id via `ErrorResponse`, so a client always knows which run it is
holding. Previously neither did, which made a lookup-by-id endpoint unusable in practice —
there was no way for a caller to ever learn the id to look up.

## Why It's Deferred, Not a Bug
The core demo value — submit an event, get a divergence analysis — doesn't depend on
retrieval. This is a genuinely separate feature: building a history view, a way to browse
or search past analyses, possibly pagination. It's scoped as an addition, not a fix.

## What It Would Take
- New `@GetMapping` route on `AnalysisController`
- Decide what's returned if the correlationId doesn't exist — 404 vs empty response
- Frontend: the id is already in the response payload and rendered on failure; a history
  view would need a list or URL-based deep link to revisit past reports

## Status
Not started. Known, intentional gap — documented here so it's a deliberate decision
to explain if asked, not an oversight to be caught off guard by.