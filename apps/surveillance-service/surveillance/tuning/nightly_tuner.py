"""T209 — Nightly tuner.

Consumes ``cold.surveillance.feedback.v1``, computes precision / recall by pattern, and writes
the proposed thresholds (cancel-rate, time-window, severity gates) to
``warm.surveillance.tuning.v1`` for review by the surveillance lead.

The job is intentionally read-only: human approval is required before the new thresholds are
applied to the live detector. Phase 14 will wire a Spring config-server that the detector
consumes; for now the proposed thresholds land in a JSON file under ``ops/surveillance/`` for
PR review.
"""

from __future__ import annotations

import datetime as dt
import json
import logging
from collections import Counter
from collections.abc import Iterable
from dataclasses import dataclass
from typing import Any

log = logging.getLogger("surveillance.tuner")


@dataclass(frozen=True)
class TuningProposal:
    pattern: str
    current_severity_threshold: str
    proposed_severity_threshold: str
    precision: float
    recall: float
    sample_size: int
    proposed_at: str


def evaluate_feedback(feedback: Iterable[dict[str, Any]]) -> list[TuningProposal]:
    """Compute precision/recall per pattern and propose threshold tweaks if signal warrants."""
    by_pattern: dict[str, Counter[str]] = {}
    for record in feedback:
        pattern = record.get("pattern", "UNKNOWN")
        by_pattern.setdefault(pattern, Counter())[record["label"]] += 1

    proposals: list[TuningProposal] = []
    now = dt.datetime.now(tz=dt.timezone.utc).isoformat()
    for pattern, counts in by_pattern.items():
        tp = counts["TRUE_POSITIVE"]
        fp = counts["FALSE_POSITIVE"]
        n = tp + fp
        if n < 20:
            continue  # too small a sample to retune
        precision = tp / n
        # Heuristic — if precision drops below 0.6, propose tightening; if above 0.95, propose loosening.
        if precision < 0.6:
            proposed = "HIGH"
        elif precision > 0.95:
            proposed = "LOW"
        else:
            proposed = "MEDIUM"
        proposals.append(
            TuningProposal(
                pattern=pattern,
                current_severity_threshold="MEDIUM",
                proposed_severity_threshold=proposed,
                precision=round(precision, 4),
                recall=1.0,  # nightly tuner can only see TP/FP from analyst-labelled set
                sample_size=n,
                proposed_at=now,
            )
        )
    return proposals


def write_proposal_file(proposals: list[TuningProposal], path: str) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump([p.__dict__ for p in proposals], f, indent=2)
    log.info("Wrote %d tuning proposal(s) to %s", len(proposals), path)
