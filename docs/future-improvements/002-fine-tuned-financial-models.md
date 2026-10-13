# Fine-Tuned Financial NLI / Embedding Models

## Current State
Using general-purpose pretrained models: `nomic-embed-text` for embeddings,
`cross-encoder/nli-deberta-v3-base` for NLI. Both run locally, zero cost, zero
external API calls — and that stays non-negotiable.

## Idea
Fine-tune both models on finance-specific text:
- Embeddings — sharper semantic similarity for financial event matching
- NLI — better contradiction detection on financial assumption language specifically,
  vs. the current general-purpose linguistic tension detection

## Why This Is a Real Project, Not a Tweak
This isn't a config change. It requires:
- A labelled dataset of financial assumption pairs (entailment / contradiction /
  neutral) — doesn't exist off-the-shelf at the scale needed, would likely require
  constructing or weakly-labelling one
- GPU access for fine-tuning at reasonable speed — CPU fine-tuning is impractically slow
- A proper evaluation methodology — held-out test set, baseline comparison against
  the current pretrained model, to prove the fine-tune is actually better and not
  just different

## Constraint
Must stay 100% local / open-source. No managed fine-tuning APIs, no cloud training
services that require an account or API key. If GPU access is needed, a free-tier
option (e.g. Colab) that doesn't compromise the "runs entirely on your machine"
philosophy of the project.

## Status
Not started. This is effectively a second, adjacent ML engineering project — distinct
from Argus's current orchestration-focused architecture — that could later plug back
in as a drop-in model swap.

## Update — Data Source Identified, Deliberately Not Pursued

Found a real candidate dataset: **FinNLI** (Financial Natural Language Inference
benchmark, arXiv 2504.16188) — 21,304 premise-hypothesis pairs sourced from SEC
filings, annual reports, and earnings call transcripts, with a 3,304-instance test
set annotated by finance experts. This would have been the right real-world dataset
to fine-tune `deberta-v3-base` against, rather than generating synthetic pairs.

Deliberately not pursued for this version of the project. Reasoning: fine-tuning on
self-generated or synthetic financial assumption pairs would have technically produced
a "fine-tuned model," but it wouldn't have been data I could fully stand behind in an
interview — it would be checking a box rather than doing real ML work. Rather than
ship a fine-tune built on questionable data, the system continues to use pretrained
DeBERTa as-is, which is honest about what it is and what it isn't.

If revisited: FinNLI is the correct starting point. Would need to verify its actual
hosting/download location (likely linked from the paper's own GitHub rather than
HuggingFace Hub directly), assess domain fit between SEC-filing language and Argus's
shorter assumption-style statements, and likely supplement with a small hand-reviewed
set in Argus's own format to bridge any style gap.

## Observed 2026-10-12 — two behaviours that motivate this

**The Contrarian never takes its escape hatch.** Across five analysed events — including
an overnight failure of a top-five bank, which has one obvious reading — the Contrarian
Agent returned `SUBSTANTIVE` every time. Never `LOW_VARIANCE`, never `DATA_GAP`.

This is the exact failure the escape hatch was designed to prevent (see ADR 007): asked
to find the strongest case that both upstream agents are wrong, Gemma 2 9B obliges even
when there is no real case to make. The consequence is narrow but real:
`CONSENSUS_HIGH_CONVICTION` requires `LOW_VARIANCE`, so it is never produced under this
prompt, and the `DATA_GAP` route into `INSUFFICIENT_SIGNAL` goes unused. The branch
itself is sound — `CONSENSUS_HIGH_CONVICTION` was produced four times while the V2/V3
prompt variants were in place, so what is missing is the input, not the logic.

The prompt instruction is necessary but not sufficient. Options worth testing: a
separate cheap classification call rather than asking the same agent to self-assess, or
a model with stronger instruction-following at this size.

**The retrieval threshold is too permissive.** A stablecoin depeg / DeFi withdrawal-halt
event matched macro events from 1977-2023 at 0.609-0.624 cosine, all clearing the 0.60
gate in `historical.py`. `nomic-embed-text` appears to give roughly 0.6 baseline
similarity to any finance-flavoured text, so scores cluster just above the line and
`HistoricalCoverage.INSUFFICIENT` almost never triggers — weak analogues are presented
as if relevant.

A domain-tuned embedding model would widen the spread. A cheaper interim fix is to raise
the threshold against measured negatives rather than leaving it at a round 0.60.

### Prompt engineering was tried first, and failed

Four variants of the Contrarian prompt were tested against the same events before
concluding the fix is structural rather than textual:

| Variant | Change | Result |
|---|---|---|
| V1 | Original: mandate first, escape hatch as a trailing caveat | 5/5 `SUBSTANTIVE` |
| V2 | Judgement-first, ordered tests, `LOW_VARIANCE` framed as a valid win | 3/3 `LOW_VARIANCE` |
| V3 | V2 with all advocacy removed, neutral framing | 3/3 `LOW_VARIANCE` |
| V4 | `SUBSTANTIVE` gated on quoting a specific claim it rejects | 1 `DEGRADED`, 1 `LOW_VARIANCE` |

Each variant produced near-uniform output, and which value it collapsed onto was decided
by the prompt's structure rather than by the event being judged. V2 and V3 differ only in
tone and produced identical behaviour, which rules out framing as the cause. V4 asked for
enough extra reasoning that the model began omitting `divergence_assessment` altogether and
failed validation twice, landing in `DEGRADED`.

The diagnosis: Gemma 2 9B is not evaluating the two upstream agents and then reporting a
verdict. It is pattern-matching to whichever option the prompt structure privileges. No
wording makes a 9B model perform a judgement it is not actually performing.

V1 is therefore retained, since its failure mode is at least the useful one — it surfaces
the `GENUINE_DIVERGENCE` and `EMPHASIS_DISPUTE` paths that make the system worth looking at.

The structural fix is to stop asking the agent to grade its own output. Two options:

- A separate, cheap classification call that sees only the two upstream theses and has no
  stake in finding a counter-argument. Removes the conflict of interest.
- Derive the assessment in code from evidence the system already computes — directional
  conflicts and NLI tensions are both available before classification runs. This is more
  consistent with ADR 004's position that divergence should not be self-reported at all.
