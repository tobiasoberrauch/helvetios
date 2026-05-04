"""T206 — Kafka source/sink for the surveillance pipeline.

Consumes ``cold.exec.fill.v1`` and ``cold.book.event.v1``, runs the layering / spoofing detector
over a 5-minute sliding window with 1-second slide, and produces ``cold.surveillance.alert.v1``
plus an analyst-feedback echo on ``cold.surveillance.feedback.v1``.

Phase 10 wraps :mod:`kafka-python` so the same code runs against Redpanda in dev and Strimzi in
prod; Phase 14 swaps in PyFlink with exactly-once checkpointing.
"""

from __future__ import annotations

import json
import logging
import os
from collections.abc import Iterable, Iterator
from typing import Any

from kafka import KafkaConsumer, KafkaProducer

from surveillance.patterns.layering_spoofing import OrderEvent, detect_layering

log = logging.getLogger("surveillance.kafka")

TOPIC_FILLS = "cold.exec.fill.v1"
TOPIC_BOOK = "cold.book.event.v1"
TOPIC_ALERTS = "cold.surveillance.alert.v1"
TOPIC_FEEDBACK = "cold.surveillance.feedback.v1"


def consume_events(bootstrap_servers: str, group_id: str = "surveillance") -> Iterator[OrderEvent]:
    """Yield :class:`OrderEvent` instances from both upstream topics."""
    consumer = KafkaConsumer(
        TOPIC_FILLS,
        TOPIC_BOOK,
        bootstrap_servers=bootstrap_servers,
        group_id=group_id,
        enable_auto_commit=False,
        value_deserializer=lambda b: json.loads(b.decode("utf-8")),
    )
    for record in consumer:
        try:
            yield _to_event(record.value)
        except (KeyError, ValueError) as exc:
            log.warning("Skipping malformed event: %s", exc)


def publish_alerts(producer: KafkaProducer, alerts: Iterable[dict[str, Any]]) -> int:
    """Publish each alert dict to ``cold.surveillance.alert.v1``. Returns the count published."""
    count = 0
    for alert in alerts:
        producer.send(TOPIC_ALERTS, key=alert["alertId"].encode(), value=json.dumps(alert).encode())
        count += 1
    producer.flush()
    return count


def publish_feedback(producer: KafkaProducer, alert_id: str, label: str, analyst: str) -> None:
    """Publish a single analyst-feedback record (true/false-positive)."""
    payload = {"alertId": alert_id, "label": label, "analyst": analyst}
    producer.send(TOPIC_FEEDBACK, key=alert_id.encode(), value=json.dumps(payload).encode())
    producer.flush()


def _to_event(raw: dict[str, Any]) -> OrderEvent:
    return OrderEvent(
        trader_id=raw["traderId"],
        instrument_isin=raw["isin"],
        side=raw["side"],
        event_type=raw["eventType"],
        quantity=float(raw["qty"]),
        price=raw.get("price") and float(raw["price"]),
        biz_time_micros=int(raw["bizTimeMicros"]),
    )


def run_local() -> int:
    """Smoke entrypoint — read from Kafka, run detector, write alerts."""
    boot = os.getenv("KAFKA_BOOTSTRAP", "localhost:9092")
    producer = KafkaProducer(bootstrap_servers=boot)
    events = list(consume_events(boot))
    alerts = list(detect_layering(events))
    return publish_alerts(producer, [alert.__dict__ for alert in alerts])


if __name__ == "__main__":  # pragma: no cover
    logging.basicConfig(level=logging.INFO)
    written = run_local()
    log.info("Wrote %d surveillance alert(s)", written)
