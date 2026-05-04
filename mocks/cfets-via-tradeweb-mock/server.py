"""T286 — CFETS-via-Tradeweb mock.

Stands in for the Tradeweb-China endpoint that fronts CFETS Bond Connect orders. Accepts an
inbound RFQ, returns a synthetic quote, and exposes a small status endpoint so integration tests
can poll the mock state.

Run::

    uv run --project mocks/cfets-via-tradeweb-mock python server.py
"""

from __future__ import annotations

import datetime as dt
import logging
import sys
import uuid

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

logging.basicConfig(stream=sys.stdout, level=logging.INFO,
                    format="%(asctime)s cfets-mock %(message)s")
log = logging.getLogger("cfets-mock")

app = FastAPI(title="CFETS-via-Tradeweb mock", version="0.1.0")
state: dict[str, dict] = {}


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "rfqs": str(len(state))}


@app.post("/cfets/rfq")
async def submit_rfq(request: Request) -> JSONResponse:
    body = await request.json()
    rfq_id = body.get("rfqId") or f"CFETS-{uuid.uuid4()}"
    state[rfq_id] = {
        "rfqId": rfq_id,
        "isin": body.get("isin"),
        "side": body.get("side"),
        "qty": body.get("qty"),
        "submittedAt": dt.datetime.now(tz=dt.timezone.utc).isoformat(),
        "status": "QUOTING",
    }
    log.info("RFQ accepted %s", rfq_id)
    return JSONResponse(state[rfq_id])


@app.get("/cfets/rfq/{rfq_id}")
def status(rfq_id: str) -> dict:
    record = state.get(rfq_id)
    if record is None:
        return {"rfqId": rfq_id, "status": "UNKNOWN"}
    # Auto-advance to QUOTED on second read so tests don't have to mutate state explicitly.
    if record["status"] == "QUOTING":
        record["status"] = "QUOTED"
        record["price"] = 100.25
        record["dealer"] = "ICBC-CIB-001"
    return record


if __name__ == "__main__":  # pragma: no cover
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8442)
