"""T145 — REGIS-TR EMIR submission mock.

Mirror of mocks/dtcc-gtr/server.py for the LuxCSD-operated REGIS-TR. The two services run
on different ports so dual-reporting can be exercised in DailyReportingBatchTest.
"""

from __future__ import annotations

import datetime as dt
import logging
import sys
import uuid

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

logging.basicConfig(stream=sys.stdout, level=logging.INFO,
                    format="%(asctime)s regis-tr %(message)s")
log = logging.getLogger("regis-tr")

app = FastAPI(title="REGIS-TR mock", version="0.1.0")


@app.post("/regis/v1/emir/submit")
async def submit(request: Request) -> JSONResponse:
    body = await request.json()
    sub_id = f"REGIS-{uuid.uuid4()}"
    log.info("EMIR REGIS accepted submission=%s reports=%d", sub_id, len(body.get("reports", [])))
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

    uvicorn.run(app, host="0.0.0.0", port=8445)
