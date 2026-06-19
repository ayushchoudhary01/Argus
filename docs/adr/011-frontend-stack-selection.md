# 011 - Frontend Stack Selection

## Status
Accepted

## Context
The Argus dashboard needs to render agent reasoning, a contradiction graph between
three agents, historical analogue data, and an assumption matrix — all from a single
DivergenceReport response. The stack needed to be enterprise-recognisable, not
over-engineered for a single-page dashboard.

## Decision
- **React + TypeScript + Vite** — already scaffolded, fast dev server, standard for
  modern dashboards
- **Tailwind CSS v4** — utility-first styling, no separate stylesheet maintenance burden
- **TanStack Query** — server state management for the analysis mutation; handles
  loading/error states without manual useState wiring
- **React Flow v11** — purpose-built for node-and-edge graphs; used for the
  Contradiction Map where agents are nodes and conflicts are edges
- **Axios** — HTTP client with a 5-minute timeout, since the pipeline involves three
  sequential/parallel LLM calls plus NLI and can take 2-3 minutes on CPU

## Consequences
- No Redux or global state library — TanStack Query covers the single async operation
  this dashboard performs; adding Redux would be unjustified complexity
- React Flow adds a dependency just for the contradiction map, but hand-rolling a
  node graph with D3 would take significantly longer for the same visual result
- Tailwind utility classes mean no separate CSS files to maintain, at the cost of
  more verbose JSX

## Alternatives Considered
- **D3.js for the contradiction map** — rejected, too low-level for a single
  three-node graph; React Flow gives the same result with a fraction of the code
- **MUI / Ant Design** — rejected, heavier dependency footprint and harder to make
  visually distinctive; Tailwind gives full control over a custom dark theme