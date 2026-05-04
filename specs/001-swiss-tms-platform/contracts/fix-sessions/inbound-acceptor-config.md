# Inbound FIX Acceptor Configuration

**Service**: `apps/inbound-fix-acceptor/`
**Engine**: Artio (high-throughput) with QuickFIX/J fallback for low-volume sessions.

Per-client session config lives in YAML files under `apps/inbound-fix-acceptor/src/main/resources/clients/<client-id>.yaml` and is loaded at startup. Updates require a service redeploy with rolling restart (per-region) so that no inbound session is dropped silently.

## YAML schema

```yaml
client:
  id: ACME-CAPITAL
  legalEntityId: 213800ABCDEFGH123456     # LEI
  status: ACTIVE                            # ACTIVE | SUSPENDED | OFFBOARDED
  preferredRegion: ZH
  fallbackRegion: LD4

session:
  senderCompId: ACME       # client's identifier (we are the acceptor)
  targetCompId: SWISSTMS   # our identifier
  fixVersion: FIX_5_0_SP2_FIXT_1_1   # or FIX_4_4
  inboundIpAllowList:
    - 203.0.113.0/24
  mtls:
    required: true
    clientCertSubject: 'CN=acme-prod, O=ACME Capital, C=US'
    clientCertFingerprintSha256: 'ab:cd:ef:…'
  encryption: TLS_1_3
  heartbeatIntervalSec: 30

throttle:
  ordersPerSecond: 1000          # per-session
  inFlightOrders: 5000
  burstWindowSec: 1
  rejectStrategy: REJECT_WITH_BMR # REJECT_WITH_BMR | DROP_SESSION

dropCopy:
  enabled: true
  separateSession: true          # FR-005e
  senderCompId: SWISSTMS-DC
  targetCompId: ACME-DC

risk:
  riskProfileId: ACME_DEFAULT_PROFILE_V3
  fatFingerNotional: 50_000_000   # USD-equivalent
  fatFingerQuantity: 1_000_000
  maxOrderSizeNotional: 25_000_000
  notionalDailyLimits:
    EQUITY: 500_000_000
    LISTED_DERIVATIVE: 100_000_000
    FIXED_INCOME: 200_000_000
    FX: 1_000_000_000
  restrictedInstruments: []        # blocklist; overrides allowlist below

permissions:
  permittedAssetClasses: [EQUITY, LISTED_DERIVATIVE, FX]
  permittedInstruments: []         # empty = all (subject to permittedAssetClasses)
  routing:
    allowedModes: [DMA, ALGO_WHEEL]    # CARE not permitted for this client
    allowedAlgos: [VWAP, TWAP, POV]
    defaultMode: DMA
    customTagMappings:
      # Per-Trax custom tag → routing-mode override
      22683: { 1: DMA, 2: ALGO_WHEEL }
```

## Semantics

- Every inbound logon is verified against `mtls.clientCertFingerprintSha256` and `inboundIpAllowList`. Mismatch → logon refused, audit entry written.
- `throttle.rejectStrategy: DROP_SESSION` is reserved for malicious behaviour (orders/sec ≥ 10× limit for ≥ 5 seconds).
- The `risk` block populates the in-process `PretradeRiskProfile` exposed to `apps/pretrade-risk-gateway/` over Aeron IPC.
- `dropCopy.separateSession: true` enforces FR-005e — order-entry and drop-copy are on distinct FIX sessions.

## Per-region distribution

Each region's acceptor loads its full client roster (the `region-router` may redirect a client's traffic at runtime). The router publishes `warm.region.handover.signal.v1` events that the acceptor honours.
