---
status: accepted
date: 2026-05-03
deciders: Architecture Review Board
---

# ADR 0002: Hexagonal-with-Venue-as-Adapter

Constitution-Anker für Principle I. Domain-Core kennt keine Venue-Protokolle.
Jeder `apps/venue-adapter-*` implementiert exakt `VenueGatewayPort` aus
`libs/domain-model/`. Mechanische Erzwingung über ArchUnit
(`tests/architecture/HexagonalArchitectureTest.java`) + CODEOWNERS.

**Trade-Off**: Latenz-Overhead durch das Port-Interface ist vernachlässigbar
(Compiler inlined). Der Vorteil: 8 Venues + 3 Clearing-Adapter mit minimalem
Risiko cross-coupling.
