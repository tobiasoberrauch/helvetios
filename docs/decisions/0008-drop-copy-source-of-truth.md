---
status: accepted
date: 2026-05-03
---

# ADR 0008: Drop-Copy ist Source-of-Truth

Constitution-Anker für Principle V. Bei Disagreements zwischen OMS-
Execution-Stream und Drop-Copy-Stream gewinnt **immer** Drop-Copy.

Reconciler-Service (`apps/reconciler-service/`) joint die Streams auf
`(SenderCompID, ClOrdID, ExecID)` mit 5-Minuten-Sliding-Window. Mismatches
gehen an `warm.recon.mismatch.v1` (AlertManager-Sev-2). Authoritativer
Stream auf `cold.exec.fill.v1` ist 1:1 der Drop-Copy-Stream.

**Mock-Variante** (Phase 3): in-process Drop-Copy-Producer im
`SixStiAdapter`. **Production** (Phase 14): separate FIX-Session pro
Venue (z.B. SIX-DC = `SWISSTMS` ↔ `SIX-DROPCOPY`).

**Drop-Copy-Venue-Matrix**:
| Venue | Drop-Copy-Support |
|---|---|
| SIX (STI) | ✓ |
| Eurex (C7) | ✓ |
| Tradeweb | ✓ |
| MarketAxess | ✓ |
| BidFX | ✓ (via Trax) |
| Bloomberg EMSX | ✓ |
| Refinitiv | ✗ — Refinitiv ist Vendor, kein Venue |
| CFETS (proxy) | via Tradeweb / Bloomberg |
