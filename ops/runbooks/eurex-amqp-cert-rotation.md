# Runbook: Eurex AMQP TLS Cert Rotation

**Sev**: 1 (expired) bis 3 (warning, > 14 Tage Vorlauf).
**Verfassungsbezug**: VI (Audit-Chain für Cert-Rotation).

## Symptome

- AlertManager: `EurexTruststoreCertExpiryWarning` / `EurexTruststoreCertExpired` / `EurexAmqpConnectionDown`.
- AMQP-Reconnect-Storm sichtbar in `clearing-adapter-eurex` Logs (`org.apache.qpid.jms.JmsConnection — failed to connect`).

## Eurex-Schedule

Eurex rotiert die CA jährlich im **September** (publik im Member-Portal,
typische Vorlaufzeit 4–6 Wochen).

## Reguläre Rotation (mit Vorlauf)

### Production-shadow / Production via cert-manager

```bash
# 1. Neue CA aus dem Eurex-Member-Portal herunterladen
curl -O https://members.eurex.com/.../eurex-ca-2027.pem

# 2. Als Secret ins Cluster einspielen
kubectl -n tms-prod-shadow-zh create secret generic eurex-truststore-2027 \
  --from-file=eurex-ca.pem=eurex-ca-2027.pem

# 3. cert-manager-Annotation auf der clearing-adapter-eurex Deployment-Spec
#    triggert reload via OpenBao PKI integration
kubectl -n tms-prod-shadow-zh annotate deploy clearing-adapter-eurex \
  swisstms.ch/truststore-version=2027 --overwrite

# 4. Rolling restart (nicht delete!) — JMS-Sessions migrieren sauber
kubectl -n tms-prod-shadow-zh rollout restart deploy clearing-adapter-eurex
kubectl -n tms-prod-shadow-zh rollout status deploy clearing-adapter-eurex --timeout=300s

# 5. Verify
kubectl -n tms-prod-shadow-zh logs -l app.kubernetes.io/name=clearing-adapter-eurex --tail=50 \
  | grep -iE 'connected|truststore'
```

### Manueller Fallback (wenn cert-manager broken ist)

```bash
keytool -import -trustcacerts -alias eurex-2027 \
  -file eurex-ca-2027.pem \
  -keystore eurex-truststore.jks -storepass <vault-secret>

kubectl cp eurex-truststore.jks tms-prod-shadow-zh/clearing-adapter-eurex-xyz:/etc/swisstms/keystores/eurex-truststore.jks

kubectl -n tms-prod-shadow-zh rollout restart deploy clearing-adapter-eurex
```

## Notfall-Rotation (Cert bereits abgelaufen)

1. Trading **nicht** automatisch pausieren — Cash-Equities und FX-Flow
   gehen über andere Adapter.
2. **Eurex-Operations-Hotline kontaktieren** (Mitgliedsnummer + LEI nennen).
3. Per Manual-Fallback (siehe oben) neuen Cert einspielen.
4. Mismatches in `cold.clearing.eurex-trade-capture.v1` während des Outages
   per Reconciler nachfahren — Drop-Copy Source-of-Truth (Constitution V).
5. Audit-Chain-Eintrag manuell anlegen:
   ```bash
   kubectl exec -n tms-prod-shadow-zh deploy/clearing-adapter-eurex -- \
     curl -X POST http://localhost:8088/admin/audit/cert-rotation-emergency \
     -d '{"alias":"eurex-2027","reason":"emergency rotation after expiry"}'
   ```

## Post-Mortem

Nach jeder ungeplanten Rotation: ADR
`docs/decisions/0xxx-pm-<datum>-eurex-cert-rotation.md` mit:
- Welcher Alarm hat (nicht?) gefeuert?
- Audit-Chain konsistent?
- Anpassung der `ALERT_THRESHOLD_DAYS` (default 30) nötig?

## Verwandte Code-Stellen

- `apps/clearing-adapter-eurex/.../CertRotationAuditor.java` — Daily-Check
- `infra/helm/clearing-adapter-eurex/templates/certificate.yaml` — cert-manager (Phase 14)
- `tests/chaos/eurex-amqp-broker-restart.yaml` — Chaos-Test (siehe Phase 16)
