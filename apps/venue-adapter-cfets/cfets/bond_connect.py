"""T282 — CFETS Bond Connect Northbound proxy.

CFETS does not expose a public API; all Northbound flow goes through one of three approved
foreign-platform proxies — Bloomberg TSOX/VCON, Tradeweb China, MarketAxess. We pick the
channel based on the bond's market segment plus the client's existing dealer access.

The proxy is intentionally thin: it normalises the inbound RFQ into a canonical Bloomberg /
Tradeweb / MarketAxess message and hands off via the matching adapter. CFETS-specific
settlement (CSDC vs HKMA-CMU) lives in ``apps/clearing-adapter-otcc``.
"""

from __future__ import annotations

import datetime as dt
import logging
import uuid
from dataclasses import dataclass
from enum import Enum

log = logging.getLogger("cfets.bond_connect")


class Channel(str, Enum):
    BLOOMBERG_TSOX = "BLOOMBERG_TSOX"
    BLOOMBERG_VCON = "BLOOMBERG_VCON"
    TRADEWEB_CHINA = "TRADEWEB_CHINA"
    MARKETAXESS = "MARKETAXESS"


@dataclass(frozen=True)
class BondConnectRfq:
    rfq_id: str
    isin: str
    side: str  # "BUY" | "SELL"
    qty: float
    settlement_date: dt.date
    channel: Channel
    client_id: str


@dataclass(frozen=True)
class BondConnectAck:
    rfq_id: str
    venue_rfq_id: str
    channel: Channel
    accepted_at: dt.datetime


def route_rfq(rfq: BondConnectRfq) -> BondConnectAck:
    """Pick the actual northbound platform and submit. Phase 15 keeps this in-process; Phase 16
    binds the real channels through the existing venue-adapter-bloomberg / -tradeweb / -marketaxess
    Java adapters via Kafka so the proxy stays venue-agnostic."""
    log.info("Bond Connect route rfq=%s channel=%s isin=%s", rfq.rfq_id, rfq.channel, rfq.isin)
    venue_rfq_id = f"{rfq.channel}-{uuid.uuid4()}"
    return BondConnectAck(
        rfq_id=rfq.rfq_id,
        venue_rfq_id=venue_rfq_id,
        channel=rfq.channel,
        accepted_at=dt.datetime.now(tz=dt.timezone.utc),
    )


def pick_default_channel(client_id: str, segment: str) -> Channel:
    """Default-channel heuristic: high-yield → MarketAxess, government → Tradeweb, OTC repo →
    Bloomberg TSOX. The client_id override (per ``clients/*.yaml``) wins in production."""
    if segment.upper() in {"HY", "EMERGING"}:
        return Channel.MARKETAXESS
    if segment.upper() in {"GOVT", "RATES"}:
        return Channel.TRADEWEB_CHINA
    return Channel.BLOOMBERG_TSOX
