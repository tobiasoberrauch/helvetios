"""Layering / Spoofing pattern detection.

Heuristik (Phase 10 — Skeleton; Phase 16 hardening):

Layering:
    Trader places multiple LIMIT-Orders auf einer Side, ohne Fill-Absicht
    (Cancel-Rate > 90% innerhalb von 1s), während gleichzeitig auf der
    anderen Side eine kleine Order gefilled wird. Mehrfaches Verschieben
    der Quotes mit gleichem Pattern → Layering-Verdacht.

Spoofing:
    Einzelne große Order auf einer Side, gefolgt von Cancel und schnellem
    Fill auf der anderen Side. Cancel-vor-Fill-Verhältnis > 95%.

Phase 10 ships die Pattern-Logik als reine Funktion mit Pandas-DataFrame-
Eingabe. Die Flink-Topology in `topology.py` wraps das mit Watermarks
und exactly-once-Semantik (Constitution V — Drop-Copy als Source-of-Truth).
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, Iterator


@dataclass(frozen=True)
class OrderEvent:
    trader_id: str
    instrument_isin: str
    side: str               # "BUY" | "SELL" | "SELL_SHORT"
    event_type: str         # "PLACED" | "CANCELED" | "FILLED"
    quantity: float
    price: float | None
    biz_time_micros: int


@dataclass(frozen=True)
class AbuseAlert:
    alert_id: str
    pattern: str            # "LAYERING" | "SPOOFING"
    severity: str           # "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
    trader_id: str
    instrument_isin: str
    window_start_micros: int
    window_end_micros: int
    evidence_event_count: int


def detect_layering(
    events: Iterable[OrderEvent],
    *,
    window_micros: int = 5_000_000,         # 5 second window
    min_layered_orders: int = 3,
    cancel_rate_threshold: float = 0.9,
) -> Iterator[AbuseAlert]:
    """Yields AbuseAlert für jeden Layering-Verdacht in der Eingabe-Sequenz.

    Algorithmus:
        1. Sortiere Events per (trader, instrument) und biz_time.
        2. Gleitendes Zeitfenster: zähle PLACED / CANCELED / FILLED pro Side.
        3. Wenn auf der EINEN Side ≥ min_layered_orders PLACED *und*
           cancel_rate ≥ threshold *und* auf der ANDEREN Side ein FILLED
           ist → Layering-Alert.
    """
    grouped: dict[tuple[str, str], list[OrderEvent]] = {}
    for ev in events:
        grouped.setdefault((ev.trader_id, ev.instrument_isin), []).append(ev)

    for (trader, isin), evs in grouped.items():
        evs.sort(key=lambda e: e.biz_time_micros)
        for i, anchor in enumerate(evs):
            window_end = anchor.biz_time_micros + window_micros
            window = [e for e in evs[i:] if e.biz_time_micros <= window_end]

            sides_placed = {"BUY": 0, "SELL": 0}
            sides_canceled = {"BUY": 0, "SELL": 0}
            sides_filled = {"BUY": 0, "SELL": 0}
            for w in window:
                side_key = "BUY" if w.side == "BUY" else "SELL"
                if w.event_type == "PLACED":
                    sides_placed[side_key] += 1
                elif w.event_type == "CANCELED":
                    sides_canceled[side_key] += 1
                elif w.event_type == "FILLED":
                    sides_filled[side_key] += 1

            for side, opposite in [("BUY", "SELL"), ("SELL", "BUY")]:
                placed = sides_placed[side]
                if placed < min_layered_orders:
                    continue
                cancel_rate = sides_canceled[side] / max(placed, 1)
                if cancel_rate < cancel_rate_threshold:
                    continue
                if sides_filled[opposite] == 0:
                    continue
                # Trigger a single alert per anchor — Phase 16 dedupes.
                yield AbuseAlert(
                    alert_id=f"layering-{trader}-{anchor.biz_time_micros}",
                    pattern="LAYERING",
                    severity="HIGH" if placed >= 5 else "MEDIUM",
                    trader_id=trader,
                    instrument_isin=isin,
                    window_start_micros=anchor.biz_time_micros,
                    window_end_micros=window_end,
                    evidence_event_count=len(window),
                )
                break
