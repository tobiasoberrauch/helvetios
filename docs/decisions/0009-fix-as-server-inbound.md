# 0009 — FIX-as-Server Inbound (Sell-Side Order Flow)

* Status: Accepted
* Date: 2026-05-04
* Deciders: Architecture Review Board
* Technical story: clarification 3

## Context and Problem Statement

We need to accept order flow from buy-side counterparties (institutional clients) over FIX. The
question was whether to accept FIX as a server (we listen, clients dial in) or to push our flow
to a sell-side prime broker that aggregates client connectivity.

## Decision Drivers

* Counterparties have published FIX dictionaries and are not going to switch to gRPC for us.
* Pre-trade risk MUST execute before any order touches our internal core.
* Latency budget for the inbound path is < 5 ms (warm tier), of which the risk gateway gets
  the lion's share.

## Decision

We **accept FIX as server** for sell-side inbound order flow:

* `apps/inbound-fix-acceptor` (Phase 13) terminates the FIX session via Artio.
* Every accepted order is handed to `apps/pretrade-risk-gateway` over Aeron IPC for fat-finger
  / kill-switch / credit-check; only orders that pass enter the OMS.
* Outbound (sell-side dropping flow into our exchange-facing adapters) reuses the same Artio
  configuration but on a separate FIX session ID.

## Alternatives Considered

* **FIX-as-client only** — would force every counterparty onto the prime broker, an
  unacceptable barrier for a learning artefact + portfolio piece.
* **Custom REST/gRPC ingest** — non-starter; the industry runs on FIX.

## Consequences

* We own session-level state (sequence numbers, gap-fill, logout) for every counterparty.
* The acceptor + risk gateway form a Single Responsibility unit; either component down
  ⇒ trading halt for that channel. Phase 14 makes both highly available.
* Audit chain captures every accept / reject decision.

## Links

* [`apps/inbound-fix-acceptor/`](../../apps/inbound-fix-acceptor/)
* [`apps/pretrade-risk-gateway/`](../../apps/pretrade-risk-gateway/)
* [`docs/decisions/0010-pretrade-risk-aeron-ipc.md`](0010-pretrade-risk-aeron-ipc.md)
