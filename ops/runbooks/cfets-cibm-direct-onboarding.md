# Runbook — CFETS CIBM Direct Onboarding (T285)

**Owner:** Sales Onboarding + Legal
**Cadence:** On-demand (per new foreign-investor client)
**Last revised:** 2026-05-04

## When to use

A foreign-investor client wants direct CIBM access (not via the Bond Connect proxy). This is
operationally heavier than Bond Connect — every step below has SLA-level dependencies on PBoC
and the Bond Connect Company Limited (BCCL).

## Onboarding stages

1. **PBoC SAFE filing**
   * Counterparty submits the QFI/RQFII application or invokes the CIBM Direct route.
   * Legal lodges the doc with SAFE; expect 4–8 weeks turnaround.
2. **BCCL onboarding**
   * Sales arranges the "BCCL onboarding stub adapter" call — currently a manual JIRA ticket;
     Phase 16 wires this into a CRM event.
   * BCCL issues the foreign-investor settlement account and the CCDC ID.
3. **CCDC + HKMA-CMU links**
   * Operations confirms the cash & securities legs route through HKMA-CMU + PBoC CNAPS.
   * Reference data (`apps/reference-data-service`) ingests the new investor LEI + CCDC ID.
4. **Risk profile**
   * Pre-trade risk gateway adds the new client to `apps/inbound-fix-acceptor/.../clients/*.yaml`
     with `permittedAssetClasses: [FIXED_INCOME]` and the agreed daily-notional limit.
5. **Test trade**
   * Sales execute a 1m-CNY government bond test trade (CGB) on the next business day after the
     last legal sign-off.
6. **Audit-chain entry**
   * The onboarding event is captured by `audit-service` with action
     `client.cibm-direct.onboarded` and the LEI + CCDC ID in the payload.

## SLA snapshot

| Stage | Owner | SLA |
|---|---|---|
| 1 | Legal | 4–8 weeks |
| 2 | Sales / BCCL | 5 business days |
| 3 | Operations | 3 business days |
| 4 | Risk | 1 business day |
| 5 | Sales | 1 business day after stage 4 |
| 6 | Audit | minutes — automated |

## See also

* [`apps/venue-adapter-cfets/cfets/bond_connect.py`](../../apps/venue-adapter-cfets/cfets/bond_connect.py) — the proxy clients normally use
* [`docs/decisions/0006-multi-region-active-active.md`](../decisions/0006-multi-region-active-active.md)
