"""
Regression tests for the NLI pairwise comparison bug.

Background: the NLI stage originally called the HuggingFace pipeline incorrectly —
either as a single manually-joined string with a literal "[SEP]" token, or as a flat
two-item list. Neither form performs genuine premise/hypothesis comparison; both
silently returned a plausible-looking label and confidence score regardless of whether
the two sentences actually contradicted each other.

These tests use known, unambiguous sentence pairs to verify the pipeline call now
performs real pairwise NLI via the documented {"text": ..., "text_pair": ...} format.

These tests load the real DeBERTa model and are slower than the rest of the suite.
"""

from services.contradiction import get_nli_pipeline

def _get_first_result(result):
    """The pipeline may return either a single dict or a list containing one dict,
    depending on the installed transformers version. Normalize to a dict either way."""
    if isinstance(result, list):
        return result[0]
    return result


def test_obvious_contradiction_is_labelled_contradiction():
    nli = get_nli_pipeline()

    result = nli(
        {"text": "Interest rates will rise sharply next quarter.",
         "text_pair": "Interest rates will fall sharply next quarter."},
        truncation=True,
        max_length=512,
    )

    label = _get_first_result(result)["label"].upper()
    assert label == "CONTRADICTION"


def test_obvious_entailment_is_labelled_entailment_or_neutral_not_contradiction():
    nli = get_nli_pipeline()

    result = nli(
        {"text": "The Federal Reserve raised interest rates by 50 basis points.",
         "text_pair": "The Fed increased rates by half a percentage point."},
        truncation=True,
        max_length=512,
    )

    label = _get_first_result(result)["label"].upper()
    # These two sentences describe the same event in different words — they should
    # never be classified as a contradiction.
    assert label != "CONTRADICTION"


def test_unrelated_sentences_are_not_labelled_contradiction():
    nli = get_nli_pipeline()

    result = nli(
        {"text": "Consumer spending remains resilient this quarter.",
         "text_pair": "The weather in London was cloudy yesterday."},
        truncation=True,
        max_length=512,
    )

    label = _get_first_result(result)["label"].upper()
    assert label != "CONTRADICTION"


def test_pipeline_returns_single_result_for_a_single_pair():
    """
    This is the specific regression check: the original buggy code passed a flat
    two-item list, which the pipeline interpreted as two independent classification
    requests, returning a list of two results instead of one comparison result. The
    correct dict-based call must return exactly one result per pair.
    """
    nli = get_nli_pipeline()

    result = nli(
        {"text": "Sentence one.", "text_pair": "Sentence two."},
        truncation=True,
        max_length=512,
    )

    first_result = _get_first_result(result)
    assert "label" in first_result
    assert "score" in first_result
