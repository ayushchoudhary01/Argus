# HuggingFace Pipeline Output Shape Varies by transformers Version

## Problem
A regression test written to verify the NLI pairwise comparison fix failed with
`AssertionError: assert 2 == 1` and `KeyError: 0`, even though the underlying NLI call
itself was correct and producing accurate labels.

## Root Cause
The test assumed `nli({"text": ..., "text_pair": ...}, ...)` always returns a list
containing one result dict — `result[0]["label"]`. On the installed `transformers`
version, calling the pipeline with a single dict input instead returns the result dict
directly, not wrapped in a list. `result[0]` on a dict raises `KeyError: 0`, and
`len(result)` on a dict returns its key count (2), not a meaningful "number of results."

## First Fix (incomplete — see below)
Added a normalizing helper in the test file that handles both possible shapes:

```python
def _get_first_result(result):
    if isinstance(result, list):
        return result[0]
    return result
```

All test assertions now go through this helper rather than assuming a fixed shape.

## The Fix Was Wrong, And It Hid A Production Bug

This was originally diagnosed as "a test bug, not a pipeline bug". That was incorrect,
and the mistaken diagnosis is the more useful half of this entry.

`services/contradiction.py` made the **same** assumption the test did:

```python
result = nli({"text": a.text, "text_pair": b.text}, truncation=True, max_length=512)
label = result[0]["label"].upper()      # KeyError: 0 when a dict comes back
```

Normalizing inside the test made the test pass while leaving production fragile. The
host and the sidecar image resolve different `transformers` builds: locally the pipeline
returned a list, so `result[0]` worked and the suite stayed green. Inside the container
it returned a bare dict, so the very first assumption pair raised `KeyError: 0`.

That exception was then swallowed by the blanket `except Exception` in
`detect_contradictions`, which returned an empty `ContradictionMap`. An empty map is
indistinguishable from "the agents agree", so every analysis run in Docker silently lost
its entire NLI stage and reported zero flagged tensions. Stage A (deterministic causal
chain comparison) carried the whole feature alone, and nothing anywhere said so.

It surfaced only once `detection_failed` was added to `ContradictionMap`, which forced
`INSUFFICIENT_SIGNAL` instead of letting a failed detection masquerade as consensus. The
first run after that change logged "Contradiction detection failed — refusing to
classify" and the real cause became visible immediately.

## Actual Fix
Give production the same normalization the test already had, in
`services/contradiction.py`:

```python
def _first(result):
    return result[0] if isinstance(result, list) else result
```

Verified after the fix: zero `KeyError` entries in the sidecar log, and an analysis that
returned `GENUINE_DIVERGENCE` with two NLI tensions at 0.998 alongside one deterministic
directional conflict — both detection stages contributing for the first time.

## Rules

**When a test and production share an assumption, and only the test breaks, fix both.**
Normalizing inside the test removed the symptom from the place that could warn us and
left it in the place that mattered. The test became *less* able to catch the bug, not
more — it now passes under either shape, so it can never fail on the mismatch that broke
production.

**A blanket `except` around a detection step converts a crash into a confident wrong
answer.** If an empty result is indistinguishable from a meaningful one, the failure must
be carried in the data, not just logged.

**Version drift between host and image is a real class of bug.** The suite passing on the
host says nothing about the shape returned inside the container. Pin it or handle both.
