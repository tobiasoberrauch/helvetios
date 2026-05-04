# Runbook: FinfraG Art. 39 Daily Submission

**Sev**: 2 (Submission scheitert) bis 3 (Reconciliation Mismatch).
**Verfassungsbezug**: VI (Audit-Chain für jede Submission).

## Tagesablauf (Norm)

| Zeit (UTC) | Schritt |
|---|---|
| 22:00 | Spring Batch Job `FinfraGArt39Job` läuft (cron `0 0 22 * * MON-FRI`) |
| 22:00–22:05 | Aggregation aus `cold.exec.fill.v1` für T-1 |
| 22:05 | XSD-Validierung (FR-027) — bei Fehlschlag Job aborts mit Sev-2 Alert |
| 22:05–22:10 | SFTP-Upload zu SIX TR Inbound (`tr.six-group.com`) |
| 22:10–06:00 (T+1) | SIX TR liefert `ack.xml` ins Outbound-Folder, Spring-Batch-Reader pickt auf |
| T+1 06:00 | `TransactionReport.status` = `ACKNOWLEDGED` (oder `REJECTED` mit Diagnose) |

## Wenn die Submission scheitert

```bash
# 1. Status checken
curl -fsS http://reporting-service:8083/api/v1/reports/FINFRAG_ART39 \
  | jq '.[] | select(.status == "REJECTED")'

# 2. Manueller Re-Run mit dryRun=true zum Validieren
curl -X POST http://reporting-service:8083/api/v1/reports/FINFRAG_ART39/run \
  -d 'reportingDate=2026-05-02&dryRun=true'

# 3. Wenn dryRun OK ist, echte Submission re-triggern
curl -X POST http://reporting-service:8083/api/v1/reports/FINFRAG_ART39/run \
  -d 'reportingDate=2026-05-02&dryRun=false'
```

## Late-Drop-Copy (T+2 oder später)

Wenn Drop-Copy einen Fill für einen bereits abgeschlossenen Reporting-Tag
nachliefert (z.B. `clOrdId` mit `bizTime > 22:00 UTC vom Vortag`):

1. Reconciler emittiert `recon.amendment` Audit-Event.
2. SIX TR akzeptiert nachträgliche Submissions per Amendment-Marker
   (siehe FinfraG Art. 39 Abs. 4).
3. Der Spring-Batch-Job hat ein dediziertes `amend`-Profil:
   ```bash
   curl -X POST http://reporting-service:8083/api/v1/reports/FINFRAG_ART39/run \
     -d 'reportingDate=2026-05-02&dryRun=false&amend=true'
   ```

## FINMA-Audit

Jährliche FINMA-Inspektion verlangt:
- Lückenlose Audit-Chain für alle Submissions (verifiziert via
  `apps/audit-service/HashChainVerifier`).
- XSD-Validation-Logs für 5 Jahre (S3 WORM bucket).
- Nachweis dass jede submission innerhalb der gesetzlichen Fristen
  erfolgte (siehe `swisstms_reporting_submission_latency_hours` Histogram).

Audit-Pack-Erzeugung in Phase 11 (US9 — analog zum RTS-25 PTP-Pack).
