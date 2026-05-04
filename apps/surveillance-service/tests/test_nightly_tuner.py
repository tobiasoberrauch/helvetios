"""Unit test for the nightly tuner heuristic."""

from __future__ import annotations

from surveillance.tuning.nightly_tuner import evaluate_feedback


def test_high_precision_loosens_threshold():
    feedback = (
        [{"pattern": "LAYERING", "label": "TRUE_POSITIVE"}] * 24
        + [{"pattern": "LAYERING", "label": "FALSE_POSITIVE"}] * 1
    )
    proposals = evaluate_feedback(feedback)
    assert len(proposals) == 1
    assert proposals[0].pattern == "LAYERING"
    assert proposals[0].proposed_severity_threshold == "LOW"
    assert proposals[0].precision >= 0.95


def test_low_precision_tightens_threshold():
    feedback = (
        [{"pattern": "SPOOFING", "label": "TRUE_POSITIVE"}] * 5
        + [{"pattern": "SPOOFING", "label": "FALSE_POSITIVE"}] * 25
    )
    proposals = evaluate_feedback(feedback)
    assert proposals[0].proposed_severity_threshold == "HIGH"


def test_small_sample_yields_no_proposal():
    feedback = [{"pattern": "LAYERING", "label": "TRUE_POSITIVE"}] * 5
    assert evaluate_feedback(feedback) == []
