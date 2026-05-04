"""T145 — DTCC GTR EMIR submission mock.

Accepts derivative trade reports over a JSON envelope and returns ack/nack mirroring
the real DTCC GTR API. Run with::

    uv run --project mocks/dtcc-gtr python server.py
"""

from __future__ import annotations

import datetime as dt
import logging
import sys
import uuid

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

logging.basicConfig(stream=sys.stdout, level=logging.INFO,
                    format="%(asctime)s dtcc-gtr %(message)s")
log = logging.getLogger("dtcc-gtr")

app = FastAPI(title="DTCC GTR mock", version="0.1.0")


@app.post("/api/emir/submit")
async def submit(request: Request) -> JSONResponse:
    body = await request.json()
    sub_id = f"DTCC-{uuid.uuid4()}"
    log.info("EMIR DTCC accepted submission=%s reports=%d", sub_id, len(body.get("reports", [])))
    return JSONResponse(
        {
            "submissionId": sub_id,
            "receivedAt": dt.datetime.now(tz=dt.timezone.utc).isoformat(),
            "status": "ACCEPTED",
        }
    )


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "UP"}


if __name__ == "__main__":  # pragma: no cover
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8444)
