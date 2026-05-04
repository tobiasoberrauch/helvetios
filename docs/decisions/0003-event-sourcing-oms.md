---
status: accepted
date: 2026-05-03
---

# ADR 0003: Outbox + Append-only event log (kein "pure event sourcing")

OMS-State lebt in `order_aggregate`; jeder Command schreibt zusätzlich in
`order_event` (append-only, hash-chained per PL/pgSQL Trigger) und in
`outbox` (Debezium → Kafka `cold.oms.event.v1`).

**Warum kein pure ES?** Aggregate-Hydration aus tausenden Events pro Order
wäre langsam und Schema-Migration-anfällig. Diese Variante gibt uns
Replay + Audit-Chain ohne den vollen ES-Komplexitäts-Tax.

**Trade-Off**: State und Events müssen konsistent sein — wir nutzen die
selbe Postgres-Transaktion für aggregat-Update + event-append +
outbox-write.
