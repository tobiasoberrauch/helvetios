---
status: accepted
date: 2026-05-03
---

# ADR 0005: QuickFIX/J + Artio statt OnixS / B2BITS / Chronicle FIX

**Decision**: QuickFIX/J 2.3.2 für Standard-Vendor-Sessions; **Artio**
(Real Logic, OSS, Aeron-basiert) für den Hot-Path Sell-Side-Inbound
(Phase 13) und High-Throughput-Adapter.

**Begründung**: OSS-Reference-Repo. QuickFIX/J + Artio decken alle
v1-Anforderungen ab. OnixS / B2BITS / Chronicle FIX sind kommerziell
und werden als Drop-In dokumentiert für Production-Hardening.

**Migrations-Pfad** (für Tier-1 Production):
1. Vendor-Sessions bleiben auf QuickFIX/J (gut genug).
2. Hot-Path-Adapter (SIX OTI, Eurex T7 ETI) wechseln zu Artio Premium
   oder Chronicle FIX (wenn Lizenz-Budget vorhanden).
3. Inbound-FIX-Acceptor unter Hochlast wechselt zu Chronicle FIX (off-heap, sub-µs).
