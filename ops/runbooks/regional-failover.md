# Runbook: Regional Failover

**Sev**: 1 (kompletter Region-Outage), 2 (lokal degraded).
**Verfassungsbezug**: FR-042c (RPO ≤ 5s, RTO ≤ 60s), Constitution V.

## Symptome

- AlertManager: `RegionDown` (Liveness-Check fail für alle Pods in einer Region für > 60s).
- AKS-Region-Status zeigt `unavailable` im Azure Portal.
- Cross-region Aurora Global DB read replica zeigt erhöhte Lag.

## Sofortmaßnahmen

### 1. Verify Region-Outage (60s)

```bash
# Aus einer anderen Region laufen lassen
kubectl --context tms-prod-shadow-ld4 get nodes
kubectl --context tms-prod-shadow-zh get nodes  # die ausgefallene Region
```

### 2. Aurora Global DB Failover

```bash
aws rds failover-global-cluster \
  --global-cluster-identifier swisstms-postgres-global \
  --target-db-cluster-identifier swisstms-postgres-ld4
```

RTO: typisch < 60s. Während Failover keine Writes möglich.

### 3. DNS / Traffic-Failover

Cloudflare-DNS-Weighted-Routing automatisch — bei manueller Override:

```bash
cloudflare-cli zone:dns:list swisstms.local | grep oms
cloudflare-cli zone:dns:update --name oms.zh --content $LD4_INGRESS_IP
```

### 4. Audit-Chain pro Region

Jede Region hat ihre eigene Audit-Chain (Constitution VI). Cross-region
Konsistenz wird beim Audit-Pack-Generieren (Phase 11) joint per
`bizTime`. Bei Region-Outage: die laufende Chain in der ausgefallenen
Region wird beim Recovery aus S3 cross-region replicated bucket
rekonstruiert.

## Recovery-Pfad

1. Ausgefallene Region zurückbringen (Azure / Helmfile sync).
2. Aurora Global DB-Topologie wiederherstellen (Regional Cluster zurück
   als Read-Replica der neuen Primary).
3. Audit-Chain-Kontinuität verifizieren.
4. Trading-Region-Routing wieder normalisieren (region-router config
   reload).

## Test

`tests/multi-region/RegionalFailoverTest.java` (Phase 14) führt einen
automatisierten Failover-Test gegen eine kind-cluster-Simulation durch.
