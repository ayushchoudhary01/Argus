import logging
from itertools import combinations

from transformers import pipeline

from models.schemas import (
    Assumption,
    CausalChain,
    ContradictionMap,
    DirectionalConflict,
    FlaggedTension,
)

logger = logging.getLogger("argus.sidecar.contradiction")

NLI_MODEL = "cross-encoder/nli-deberta-v3-base"
NLI_THRESHOLD = 0.85

_nli_pipeline = None


def get_nli_pipeline():
    global _nli_pipeline
    if _nli_pipeline is None:
        _nli_pipeline = pipeline("text-classification", model=NLI_MODEL, device=-1)
    return _nli_pipeline


def _detect_directional_conflicts(causal_chains: list[CausalChain]) -> tuple[list[DirectionalConflict], set[tuple[str, str]]]:
    conflicts = []
    skipped_pairs: set[tuple[str, str]] = set()

    for a, b in combinations(causal_chains, 2):
        if a.target_variable.lower() == b.target_variable.lower() and a.direction != b.direction:
            conflicts.append(
                DirectionalConflict(
                    premise=f"{a.agent}: {a.direction.value} on {a.target_variable}",
                    hypothesis=f"{b.agent}: {b.direction.value} on {b.target_variable}",
                    agents=[a.agent, b.agent],
                    target_variable=a.target_variable,
                )
            )
            skipped_pairs.add((a.agent, b.agent))
            skipped_pairs.add((b.agent, a.agent))

    return conflicts, skipped_pairs


def _first(result):
    """Normalise the HuggingFace pipeline's return shape.

    Depending on the installed transformers version the pipeline returns either a
    single dict or a list containing one dict. Indexing a dict with [0] raises
    KeyError: 0, which the caller's exception handler then turns into a flagged
    detection failure — so every analysis loses its NLI stage silently.
    """
    return result[0] if isinstance(result, list) else result


def _detect_nli_tensions(
    assumptions: list[Assumption],
    skipped_pairs: set[tuple[str, str]],
) -> tuple[list[FlaggedTension], list[dict], list[dict]]:
    nli = get_nli_pipeline()
    flagged_tensions = []
    entailments = []
    neutral_pairs = []

    for a, b in combinations(assumptions, 2):
        if (a.agent, b.agent) in skipped_pairs:
            continue
        if a.agent == b.agent:
            continue

        result = _first(nli({"text": a.text, "text_pair": b.text}, truncation=True, max_length=512))
        label = result["label"].upper()
        score = round(result["score"], 4)

        if label == "CONTRADICTION" and score >= NLI_THRESHOLD:
            flagged_tensions.append(
                FlaggedTension(
                    premise=a,
                    hypothesis=b,
                    probability=score,
                )
            )
        elif label == "ENTAILMENT":
            entailments.append({"premise": a.model_dump(), "hypothesis": b.model_dump(), "probability": score})
        else:
            neutral_pairs.append({"premise": a.model_dump(), "hypothesis": b.model_dump(), "probability": score})

    return flagged_tensions, entailments, neutral_pairs


def detect_contradictions(
    assumptions: list[Assumption],
    causal_chains: list[CausalChain],
) -> ContradictionMap:
    try:
        directional_conflicts, skipped_pairs = _detect_directional_conflicts(causal_chains)
        flagged_tensions, entailments, neutral_pairs = _detect_nli_tensions(assumptions, skipped_pairs)

        return ContradictionMap(
            directional_conflicts=directional_conflicts,
            flagged_tensions=flagged_tensions,
            entailments=entailments,
            neutral_pairs=neutral_pairs,
        )

    except Exception:
        logger.exception("Failed to detect contradictions")
        # An empty map is indistinguishable from "the agents agree", which would
        # silently upgrade the result to CONSENSUS_HIGH_CONVICTION. Flag it so the
        # rules engine can refuse to classify instead.
        return ContradictionMap(
            directional_conflicts=[],
            flagged_tensions=[],
            entailments=[],
            neutral_pairs=[],
            detection_failed=True,
        )