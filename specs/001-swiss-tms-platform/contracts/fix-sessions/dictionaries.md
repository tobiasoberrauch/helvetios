# FIX Data Dictionaries

Every venue has its own dictionary in `contracts/fix/venues/`. Vendored from the venue's published spec at the documented version. Updates require:

1. Drop the new XML in place.
2. Update the per-venue `JdbcStoreFactory` dictionary path.
3. Rebuild — Gradle codegen regenerates message classes.
4. Update or add property tests in `tests/property/java/.../venue/<venue>/`.
5. Add a row to `docs/decisions/0xxx-fix-dialect-update-<venue>-<date>.md` ADR with the diff summary.

## In scope

| Venue | File | Version |
|---|---|---|
| FIX standard 4.4 | `contracts/fix/FIX44.xml` | QuickFIX 2.3.2 distribution |
| FIX standard 5.0 SP2 | `contracts/fix/FIX50SP2.xml` | QuickFIX 2.3.2 distribution |
| FIX standard FIXT.1.1 | `contracts/fix/FIXT11.xml` | QuickFIX 2.3.2 distribution |
| SIX STI | `contracts/fix/venues/SIX_STI_FIX44.xml` | SIX-published, latest |
| Eurex T7 FIX gateway | `contracts/fix/venues/EUREX_T7_FIX42.xml` | Eurex T7 release notes, latest |
| Tradeweb TradeXpress | `contracts/fix/venues/TRADEWEB_TradeXpress.xml` | OnixS-published v101.34 |
| MarketAxess Open Trading | `contracts/fix/venues/MARKETAXESS_OPEN_TRADING.xml` | MarketAxess-published, latest |
| MarketAxess Trax APA | `contracts/fix/venues/TRAX_APA_FIX50SP2.xml` | EP228 + Trax custom tags 22683 etc. |
| Bloomberg EMSX (fallback) | `contracts/fix/venues/BLOOMBERG_EMSX_FIX44.xml` | Bloomberg-published, latest |

## Custom tags inventory

Custom tags introduced or used by this platform are documented here. Tag numbers are chosen from the FIX-reserved user-defined range (5000–9999) with no overlap with venue custom tags.

| Tag | Name | Defined by | Purpose |
|---|---|---|---|
| 7777 | InternalTraceParent | swiss-tms | OpenTelemetry W3C traceparent propagation across FIX |
| 7778 | InternalRegion | swiss-tms | The region that processed the order (set by region-router) |
| 7779 | InternalRoutingMode | swiss-tms | DMA / ALGO_WHEEL / CARE marker for downstream services |
| 7780 | InternalAlgoStrategy | swiss-tms | Algo strategy id for algo-wheel orders |
| 22683 | TraxRoutingHint | MarketAxess Trax | (venue-defined) — used in inbound mappings |

A registry test in `tests/property/java/fix-tag-registry-no-collision-test.java` enforces that no two contexts use the same custom tag with different semantics.
