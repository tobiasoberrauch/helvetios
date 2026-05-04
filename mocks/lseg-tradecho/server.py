"""T144 — LSEG TRADEcho ARM mock (RTS-22 submission target).

Tiny FastAPI listener that accepts MiFID-II RTS-22 transaction reports over HTTPS, validates
that the body has an ``auth.016.001.02`` envelope, and replies with an ARM acknowledgement
mirroring the field set the real LSEG TRADEcho returns. Used by ``DailyReportingBatchTest``
(T131) and ``Rts22JobIntegrationTest``.

Run locally::

    uv run --project mocks/lseg-tradecho python server.py
"""

from __future__ import annotations

import datetime as dt
import hashlib
import logging
import sys
import uuid
import xml.etree.ElementTree as ET

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

logging.basicConfig(stream=sys.stdout, level=logging.INFO,
                    format="%(asctime)s lseg-tradecho %(message)s")
log = logging.getLogger("lseg-tradecho")

app = FastAPI(title="LSEG TRADEcho ARM mock", version="0.1.0")


@app.post("/api/v1/transactions")
async def submit_rts22(request: Request) -> JSONResponse:
    body = await request.body()
    sha = hashlib.sha256(body).hexdigest()
    try:
        root = ET.fromstring(body)
    except ET.ParseError as exc:
        raise HTTPException(status_code=400, detail=f"malformed XML: {exc}")
    if "auth.016.001.02" not in (root.tag or ""):
        raise HTTPException(status_code=415, detail="not an auth.016.001.02 envelope")

    submission_id = f"LSEG-{uuid.uuid4()}"
    log.info("RTS-22 accepted submission=%s sha256=%s", submission_id, sha)
    return JSONResponse(
        {
            "submissionId": submission_id,
            "receivedAt": dt.datetime.now(tz=dt.timezone.utc).isoformat(),
            "sha256": sha,
            "status": "RECEIVED",
        }
    )


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "UP"}


if __name__ == "__main__":  # pragma: no cover
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8443)
