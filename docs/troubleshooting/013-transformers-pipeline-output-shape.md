# HuggingFace Pipeline Output Shape Varies by transformers Version

## Problem
A regression test written to verify the NLI pairwise comparison fix failed with
`AssertionError: assert 2 == 1` and `KeyError: 0`, even though the underlying NLI call
itself was correct and producing accurate labels.

## Root Cause
The test assumed `nli({"text": ..., "text_pair": ...}, ...)` always returns a list
containing one result dict — `result[0]["label"]`. On the installed `transformers`
version, calling the pipeline with a single dict input instead returns the result dict
directly, not wrapped in a list. `result[0]` on a dict returns a `KeyError`, and
`len(result)` on a dict returns its key count (2), not a meaningful "number of results."

This was a test bug, not a pipeline bug — the actual NLI fix (using the documented
`{"text": ..., "text_pair": ...}` format) was already working correctly; the test's
assumption about the response shape was wrong.

## Fix
Added a normalizing helper in the test file that handles both possible shapes:

```python
def _get_first_result(result):
    if isinstance(result, list):
        return result[0]
    return result
```

All assertions now go through this helper rather than assuming a fixed shape.

## Rule
When writing tests against a third-party library's output (especially ML libraries
where APIs evolve across versions), don't assume the return shape from documentation or
memory — print/inspect the actual return value once, or write the test defensively
against multiple plausible shapes, particularly for single-item vs single-input calls
where wrap-in-a-list behavior is a common source of version drift.