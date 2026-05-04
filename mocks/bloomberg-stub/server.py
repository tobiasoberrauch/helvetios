"""T181 — Bloomberg BLPAPI four-service mock.

Stands in for ``//blp/refdata``, ``//blp/mktdata``, ``//blp/apiauth`` and ``//blp/emapisvc``.
Real BLPAPI is a proprietary binary protocol over localhost:8194; here we expose REST-shaped
endpoints with the same conceptual operations so the venue-adapter-bloomberg can be wired up
end-to-end in dev / CI.

Run::

    uv run --project mocks/bloomberg-stub python server.py
"""

from __future__ import annotations

import datetime as dt
import logging
import random
import sys
import uuid

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

logging.basicConfig(stream=sys.stdout, level=logging.INFO,
                    format="%(asctime)s bloomberg-stub %(message)s")
log = logging.getLogger("bloomberg-stub")

app = FastAPI(title="Bloomberg BLPAPI mock", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


# ---- //blp/apiauth -----------------------------------------------------------
@app.post("/blp/apiauth/identity")
def auth(req: dict) -> dict:
    uuid_in = req.get("uuid", 0)
    log.info("apiauth identity uuid=%s", uuid_in)
    return {
        "uuid": uuid_in,
        "permissions": ["BPS:LIVE", "EQ:CH", "EQ:DE", "FX:G10"],
        "expiresAt": (dt.datetime.now(tz=dt.timezone.utc)
                      + dt.timedelta(hours=24)).isoformat(),
    }


# ---- //blp/refdata -----------------------------------------------------------
@app.post("/blp/refdata/request")
def refdata(req: dict) -> dict:
    tickers = req.get("tickers", [])
    fields = req.get("fields", [])
    log.info("refdata request tickers=%d fields=%d", len(tickers), len(fields))
    out = {}
    for t in tickers:
        out[t] = {f: f"STUB-{t}-{f}" for f in fields}
    return {"data": out}


# ---- //blp/mktdata (snapshot) ------------------------------------------------
@app.get("/blp/mktdata/{topic}")
def mktdata(topic: str) -> dict:
    return {
        "topic": topic,
        "BID": round(80 + random.random() * 40, 4),
        "ASK": round(80 + random.random() * 40, 4),
        "LAST_PRICE": round(80 + random.random() * 40, 4),
        "ts": int(dt.datetime.now(tz=dt.timezone.utc).timestamp() * 1000),
    }


# ---- //blp/emapisvc ----------------------------------------------------------
@app.post("/blp/emapisvc/createOrderAndRouteEx")
async def emsx_create(request: Request) -> JSONResponse:
    body = await request.json()
    log.info(
        "emsx CreateOrderAndRouteEx trader=%s ticker=%s qty=%s side=%s broker=%s",
        body.get("trader"), body.get("ticker"), body.get("qty"),
        body.get("side"), body.get("routeBroker"),
    )
    return JSONResponse(
        {
            "orderId": f"ORD-{uuid.uuid4()}",
            "routeId": f"RTE-{uuid.uuid4()}",
            "status": "ACCEPTED",
        }
    )


if __name__ == "__main__":  # pragma: no cover
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8194)
