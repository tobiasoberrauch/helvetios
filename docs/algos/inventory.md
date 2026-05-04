# Algorithmic Trading Inventory (RTS-6/7)

Per MiFID-II RTS-6/7, every algorithmic trading strategy must be inventoried with:

- A unique identifier
- A human-readable description
- A current owner (named individual)
- An approval stamp from the responsible MD / Head of Quants
- Last review date

The list below is the platform's reference set. Production deployments must be reviewed against the bank's own register at onboarding.

## Algos in scope

Filled in **Phase 13** alongside the SOR / EMS / inbound-acceptor implementations.

| ID | Strategy | Owner | Approval | Last review |
|---|---|---|---|---|
| ALGO-VWAP-V1 | Volume-Weighted Average Price | _TBD Phase 13_ | _TBD Phase 13_ | — |
| ALGO-TWAP-V1 | Time-Weighted Average Price | _TBD Phase 13_ | _TBD Phase 13_ | — |
| ALGO-POV-V1  | Percent-of-Volume | _TBD Phase 13_ | _TBD Phase 13_ | — |
| ALGO-IS-V1   | Implementation Shortfall | _TBD Phase 13_ | _TBD Phase 13_ | — |
| ALGO-AIEX-V1 | Tradeweb AiEX rule-engine wrapper | _TBD Phase 9 (US7)_ | _TBD Phase 9_ | — |

Each algo MUST also have:

- Pre-trade risk profile (in `apps/pretrade-risk-gateway/`).
- Kill-switch scope (in `apps/entitlements-service/`).
- ADR justifying its design (under `docs/decisions/`).
