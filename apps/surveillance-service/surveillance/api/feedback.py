"""T208 — REST API for analyst feedback marking.

Tiny FastAPI app the surveillance dashboard talks to:

* ``GET  /alerts/{alert_id}``   — read one alert
* ``POST /alerts/{alert_id}/label`` — mark TRUE_POSITIVE / FALSE_POSITIVE / UNDETERMINED
* ``GET  /metrics/labels``      — count per label since midnight (input for nightly tuner)

Feedback is published to ``cold.surveillance.feedback.v1`` so the nightly tuner sees it without
querying OpenSearch directly.
"""

from __future__ import annotations

import datetime as dt
import logging
from collections import Counter
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s surveillance-api %(message)s")
log = logging.getLogger("surveillance-api")

app = FastAPI(title="Swiss-TMS Surveillance Feedback API", version="0.1.0")


class LabelRequest(BaseModel):
    label: str = Field(pattern=r"^(TRUE_POSITIVE|FALSE_POSITIVE|UNDETERMINED)$")
    analyst: str
    notes: str | None = None


# In-memory store — Phase 14 swaps in OpenSearch + Kafka.
_ALERTS: dict[str, dict[str, Any]] = {}
_FEEDBACK: list[dict[str, Any]] = []


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.put("/alerts/{alert_id}", status_code=201)
def upsert_alert(alert_id: str, alert: dict[str, Any]) -> dict[str, Any]:
    alert.setdefault("alert_id", alert_id)
    _ALERTS[alert_id] = alert
    return alert


@app.get("/alerts/{alert_id}")
def get_alert(alert_id: str) -> dict[str, Any]:
    alert = _ALERTS.get(alert_id)
    if alert is None:
        raise HTTPException(status_code=404, detail=f"Alert {alert_id} not found")
    return alert


@app.post("/alerts/{alert_id}/label")
def label_alert(alert_id: str, body: LabelRequest) -> dict[str, Any]:
    alert = _ALERTS.get(alert_id)
    if alert is None:
        raise HTTPException(status_code=404, detail=f"Alert {alert_id} not found")
    alert["analyst_label"] = body.label
    alert["analyst_id"] = body.analyst
    alert["labelled_at"] = dt.datetime.now(tz=dt.timezone.utc).isoformat()
    record = {"alertId": alert_id, "label": body.label, "analyst": body.analyst,
              "notes": body.notes, "labelledAt": alert["labelled_at"]}
    _FEEDBACK.append(record)
    log.info("Alert %s labelled %s by %s", alert_id, body.label, body.analyst)
    return {"alertId": alert_id, "label": body.label, "status": "RECORDED"}


@app.get("/metrics/labels")
def label_counts() -> dict[str, int]:
    today = dt.datetime.now(tz=dt.timezone.utc).date().isoformat()
    counts: Counter[str] = Counter(
        f["label"] for f in _FEEDBACK if f["labelledAt"].startswith(today)
    )
    return dict(counts)


@app.get("/feedback")
def feedback() -> list[dict[str, Any]]:
    return list(_FEEDBACK)
