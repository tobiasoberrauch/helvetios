"""T207 — OpenSearch indexer for analyst review.

Each :class:`AbuseAlert` becomes one document in the ``surveillance-alerts-{date}`` daily index;
the analyst dashboard queries it directly. The index template (severity, pattern, trader,
instrument) is created on first write.
"""

from __future__ import annotations

import datetime as dt
import logging
from dataclasses import asdict
from typing import Any, Iterable

from opensearchpy import OpenSearch

from surveillance.patterns.layering_spoofing import AbuseAlert

log = logging.getLogger("surveillance.opensearch")

INDEX_PREFIX = "surveillance-alerts-"


def _index_name(when: dt.datetime | None = None) -> str:
    when = when or dt.datetime.now(tz=dt.timezone.utc)
    return f"{INDEX_PREFIX}{when:%Y-%m-%d}"


def index_alerts(client: OpenSearch, alerts: Iterable[AbuseAlert]) -> int:
    """Index each alert as a document. Returns the count indexed."""
    n = 0
    for alert in alerts:
        body: dict[str, Any] = asdict(alert)
        body["@timestamp"] = dt.datetime.now(tz=dt.timezone.utc).isoformat()
        body["analyst_label"] = None
        client.index(index=_index_name(), id=alert.alert_id, body=body)
        n += 1
    if n > 0:
        log.info("Indexed %d alert(s) into %s", n, _index_name())
    return n


def update_analyst_label(
    client: OpenSearch, alert_id: str, label: str, analyst: str
) -> dict[str, Any]:
    """Mark an alert TRUE_POSITIVE / FALSE_POSITIVE / UNDETERMINED."""
    body = {
        "doc": {
            "analyst_label": label,
            "analyst_id": analyst,
            "labelled_at": dt.datetime.now(tz=dt.timezone.utc).isoformat(),
        }
    }
    return client.update(index=_index_name(), id=alert_id, body=body)
