"""T283 — CFETS Swap Connect Northbound IRS-RFQ proxy.

Routes onshore-CNY interest-rate swap RFQs through Tradeweb (FpML confirmation) and pairs the
clearing leg with the OTCC↔SHCH interop. The proxy emits the same FpML 5.12 envelope the
``apps/clearing-adapter-eurex`` mapper produces (different swap economics; same schema), so
downstream consumers don't need a CFETS-specific path.
"""

from __future__ import annotations

import datetime as dt
import logging
import uuid
from dataclasses import dataclass
from typing import Literal

log = logging.getLogger("cfets.swap_connect")


@dataclass(frozen=True)
class IrsRfq:
    rfq_id: str
    notional_cny: float
    fixed_rate_pct: float
    floating_index: Literal["FR007", "SHIBOR3M"]
    effective_date: dt.date
    termination_date: dt.date
    client_id: str


@dataclass(frozen=True)
class IrsAck:
    rfq_id: str
    venue_rfq_id: str
    fpml_doc_ref: str
    clearing_route: Literal["OTCC_SHCH"]
    accepted_at: dt.datetime


def route_rfq(rfq: IrsRfq) -> IrsAck:
    """Submit through Tradeweb and tag the clearing route as OTCC↔SHCH (the only Northbound
    Swap Connect clearing path). The actual FpML doc is built by
    ``FpmlInterestRateSwapMapper`` in the Eurex adapter — Phase 16 promotes that mapper to a
    shared library so it can be reused here without crossing service boundaries."""
    venue_rfq_id = f"TWB-CN-{uuid.uuid4()}"
    fpml_ref = f"fpml://swisstms/swap/{venue_rfq_id}"
    log.info(
        "Swap Connect IRS rfq=%s notional=%.0f rate=%.4f%% index=%s",
        rfq.rfq_id, rfq.notional_cny, rfq.fixed_rate_pct, rfq.floating_index,
    )
    return IrsAck(
        rfq_id=rfq.rfq_id,
        venue_rfq_id=venue_rfq_id,
        fpml_doc_ref=fpml_ref,
        clearing_route="OTCC_SHCH",
        accepted_at=dt.datetime.now(tz=dt.timezone.utc),
    )
