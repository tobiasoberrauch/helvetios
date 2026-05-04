"""T284 — Bond Connect Repo (live since 10-Feb-2025) booking workflow.

Bond Connect Repo lets foreign investors enter onshore-CNY repo positions against eligible
CIBM bonds. The booking is governed by GMRA (Global Master Repurchase Agreement) templates;
each trade carries a reference to the GMRA version + counterparty annex.

The proxy stitches the trade leg into a Tradeweb-style FIX TradeCaptureReport(35=AE) and
attaches the GMRA reference so the back-office reconciliation has the right legal trail.
"""

from __future__ import annotations

import datetime as dt
import logging
import os
from dataclasses import dataclass
from pathlib import Path

log = logging.getLogger("cfets.repo")


@dataclass(frozen=True)
class RepoBooking:
    booking_id: str
    isin: str
    notional_cny: float
    repo_rate_pct: float
    start_date: dt.date
    maturity_date: dt.date
    counterparty_lei: str
    gmra_version: str          # e.g. "GMRA-2011"
    gmra_annex: str            # e.g. "PRC_CIBM_Annex_v3"


@dataclass(frozen=True)
class RepoAck:
    booking_id: str
    fix_message_ref: str
    gmra_doc_ref: str
    accepted_at: dt.datetime


GMRA_TEMPLATE_DIR = Path(
    os.environ.get("SWISSTMS_GMRA_DIR", "contracts/legal/gmra")
)


def book(repo: RepoBooking) -> RepoAck:
    """Phase 15 emits the booking record + GMRA reference; Phase 16 wires the actual FIX
    submission through ``venue-adapter-tradeweb`` and persists the GMRA-stamped doc to S3 WORM."""
    template = GMRA_TEMPLATE_DIR / f"{repo.gmra_version}/{repo.gmra_annex}.md"
    if not template.exists():
        log.warning(
            "GMRA template missing %s — booking proceeds, doc-ref will be a placeholder",
            template,
        )
    log.info(
        "Repo booking id=%s isin=%s notional=%.0f rate=%.3f%% maturity=%s",
        repo.booking_id,
        repo.isin,
        repo.notional_cny,
        repo.repo_rate_pct,
        repo.maturity_date,
    )
    return RepoAck(
        booking_id=repo.booking_id,
        fix_message_ref=f"fix://swisstms/repo/{repo.booking_id}",
        gmra_doc_ref=str(template),
        accepted_at=dt.datetime.now(tz=dt.timezone.utc),
    )
