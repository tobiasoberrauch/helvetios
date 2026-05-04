---
status: accepted
date: 2026-05-03
---

# ADR 0004: Aeron für Hot-Path, Kafka für Warm/Cold

**Decision**: Aeron IPC + SBE + Disruptor für sub-100µs Hot-Path. Kafka
3.7 (KRaft) für warm + cold tier (drop-copy fan-out, OMS events,
surveillance, reporting).

**Tier-1-Evidenz**: HSBC Equities (Grahame Rogers, Aeron MeetUp 2024),
Man Group FX ($1.5T/Jahr), Brevan Howard, SIX SIC5, EDX/EDXM
(73µs RTT), Coinbase. Kafka: ING, RBC, Capital One, Robinhood,
Euronext (Optiq), anonymes Tier-1 mit 1.6M msg/s.

**Kein Kafka im Hot-Path** — Kafka p99 5–15ms, das macht den
sub-100µs-Tick-to-Trade-Anspruch unmöglich.

**Drop-Ins**: Solace PubSub+ (Multi-Region Mesh), Aeron Premium
(noch tighter latency), Chronicle Queue (in-process compliance journal).
