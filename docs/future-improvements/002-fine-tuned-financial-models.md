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