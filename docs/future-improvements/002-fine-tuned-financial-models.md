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