# NLI Stage Never Performed Genuine Pairwise Comparison

## Problem
The contradiction detection stage (Stage B, DeBERTa NLI) was producing plausible-looking
labels and probability scores (e.g. 0.9977) for every assumption pair — but those scores
never reflected a genuine premise/hypothesis comparison between two assumptions.

## Root Cause
The HuggingFace `text-classification` pipeline does not perform pairwise NLI when given
either a single concatenated string or a flat list of two strings. Two broken forms were
used at different points in this project, both incorrect for different reasons:

**Form 1 (string concatenation with manual [SEP]):**
```python
result = nli(f"{a.text} [SEP] {b.text}", truncation=True, max_length=512)
```
This tokenizes the entire string — including the literal characters `[SEP]` — as a single
input sequence. The model has no architectural understanding that this is a premise and
a hypothesis; it just classifies one long sentence.

**Form 2 (flat list of two strings):**
```python
result = nli([a.text, b.text], truncation=True, max_length=512)
```
This is interpreted as two independent classification requests — "classify a.text" and
"classify b.text" — not a comparison. The code only read `result[0]`, silently using the
classification of `a.text` alone as if it were the comparison score.

Neither form crashed. Both returned a JSON-shaped result indistinguishable from a correct
one. This is what made the bug dangerous — no error, no stack trace, nothing visible in
logs. It would only be caught by deliberately testing two sentences with a known, obvious
contradiction and checking whether the label was actually correct.

## Fix
Use the pipeline's documented dict-based premise/hypothesis API:
```python
result = nli({"text": a.text, "text_pair": b.text}, truncation=True, max_length=512)
```

## Detection
Caught via external code review, not via testing — the bug produced no errors and no
obviously wrong-looking output. Every `FLAGGED_TENSION` and probability score generated
before this fix should be considered unverified, not ground truth.

## Rule
When using a HuggingFace pipeline for any sentence-pair task (NLI, similarity, etc.),
always verify against the model's official model card or the transformers docs for the
correct input format. Never assume a list or a manually-joined string achieves a pairwise
comparison — confirm the documented API explicitly supports paired text classification
before relying on its output.