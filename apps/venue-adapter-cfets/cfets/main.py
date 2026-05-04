"""CFETS Northbound proxy.

Northbound-Wege zu CFETS:
  1. Bond Connect via Bloomberg TSOX/VCON
  2. Bond Connect via Tradeweb China
  3. Bond Connect via MarketAxess
  4. Swap Connect via Tradeweb (FpML confirmation)
  5. Bond Connect Repo (since 10-Feb-2025) — GMRA-based booking

CFETS-Protokolle sind nicht öffentlich; alle Northbound läuft über
Drittplattformen, daher dieser Adapter ist ein leichtgewichtiger
Routing-Proxy.
"""

from __future__ import annotations

from enum import Enum

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel


class NorthboundChannel(str, Enum):
    BLOOMBERG_TSOX = "BLOOMBERG_TSOX"
    BLOOMBERG_VCON = "BLOOMBERG_VCON"
    TRADEWEB_CHINA = "TRADEWEB_CHINA"
    MARKETAXESS    = "MARKETAXESS"


class CfetsBondOrder(BaseModel):
    cl_ord_id: str
    cfets_symbol: str         # CFETS bond code
    side: str                  # BUY | SELL
    quantity: float
    price: float
    channel: NorthboundChannel = NorthboundChannel.TRADEWEB_CHINA


class CfetsRoutingDecision(BaseModel):
    cl_ord_id: str
    routed_to: str              # which downstream adapter (venue-adapter-tradeweb etc.)
    cfets_symbol: str
    domain_isin: str            # mapped from CFETS symbology to ISIN

app = FastAPI(title="Swiss-TMS CFETS Northbound Proxy", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "adapter": "venue-adapter-cfets"}


@app.post("/orders/route", response_model=CfetsRoutingDecision)
def route_order(order: CfetsBondOrder) -> CfetsRoutingDecision:
    """Map CFETS-symbol → ISIN and decide which downstream adapter delivers
    the order to CFETS via Bond/Swap Connect."""
    isin = _cfets_to_isin(order.cfets_symbol)
    if isin is None:
        raise HTTPException(status_code=404,
                             detail=f"CFETS symbol {order.cfets_symbol} not in instrument-master")

    target = {
        NorthboundChannel.BLOOMBERG_TSOX: "venue-adapter-bloomberg",
        NorthboundChannel.BLOOMBERG_VCON: "venue-adapter-bloomberg",
        NorthboundChannel.TRADEWEB_CHINA: "venue-adapter-tradeweb",
        NorthboundChannel.MARKETAXESS:    "venue-adapter-marketaxess",
    }[order.channel]

    return CfetsRoutingDecision(
        cl_ord_id=order.cl_ord_id,
        routed_to=target,
        cfets_symbol=order.cfets_symbol,
        domain_isin=isin,
    )


def _cfets_to_isin(cfets_symbol: str) -> str | None:
    """Phase 15 — Stub. Real reference-data lookup via reference-data-service."""
    stub_table = {
        "190001.IB": "CN1900014891",   # demo CGB
        "190002.IB": "CN1900024899",
    }
    return stub_table.get(cfets_symbol)
