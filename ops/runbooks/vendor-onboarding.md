# Runbook: Vendor / Venue Onboarding

Für jede neue Vendor-Lizenz oder Venue-Mitgliedschaft.

## Bloomberg

1. Mitgliedsantrag an Bloomberg LP.
2. Nach Approval: BLPAPI v3 JAR aus Member-Portal herunterladen,
   in `infra/maven-mirror/` als private Maven coordinate ablegen.
3. UUID + SerialNumber pro Service-Account anlegen.
4. EMRS-Sync konfigurieren in `entitlements-service`.

## Refinitiv (LSEG)

1. RTSDK-Lizenz beantragen.
2. RDP OAuth2 Client-Credentials anfordern; in OpenBao ablegen.
3. DACS-PE-Codes pro AppId zuweisen (durch Refinitiv-Sales).
4. Pro Service: eindeutige AppId vergeben.

## SIX

1. Mitgliedsantrag bei SIX Swiss Exchange.
2. STI / OTI / QTI / IMI / MDDX-Zugänge separat beantragen.
3. SSL-Client-Certs per CSR ausstellen lassen.
4. Sequence-Number-Bookkeeping in Postgres `fix_session_state` anlegen.

## Eurex

1. Eurex Clearing Member Section onboarden.
2. AMQP 1.0 Endpunkt-Credentials erhalten.
3. JKS Truststore mit Eurex CA aus Member-Portal beziehen.
4. cert-manager konfigurieren für Auto-Renewal (siehe
   `eurex-amqp-cert-rotation.md`).

## MarketAxess Trax APA

1. APA-Lizenz beantragen.
2. SenderCompID + TargetCompID koordinieren.
3. EP228 + Trax-Custom-Tags-Spec bestätigen.
4. Daily-Reset-Window (23:00–23:05 GMT) im Schedule berücksichtigen.

## Testing

Jeder Vendor / Venue MUSS in der Phase-3-Conformance-Test-Suite ein
mindestens basic lifecycle test haben. Bei UAT-Setup gegen den realen
Vendor-Sandbox-Endpunkt: Tests in CI per `--profile vendor-sandbox`
selektiv aktivieren.
