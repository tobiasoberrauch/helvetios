# Avro Topic Schemas (Apicurio Registry)

Jede Datei hier entspricht **einer Topic-Version**. Format: `<topic-name>.<version>.avsc`.

## Naming-Convention

Topics folgen `<tier>.<context>.<event>.v<n>` (siehe
[contracts/kafka-topics/topic-naming.md](../../specs/001-swiss-tms-platform/contracts/kafka-topics/topic-naming.md)).

Schema-Datei dazu: `hot/<topic>.v1.avsc` / `warm/<topic>.v1.avsc` / `cold/<topic>.v1.avsc`.

## Apicurio Compatibility

Apicurio läuft im Modus `BACKWARD_TRANSITIVE` — ein Konsument von v3 muss
v1 und v2 lesen können. Breaking Changes erzwingen einen neuen
`v<n+1>`-File und einen mindestens eintägigen Koexistenz-Zeitraum
(Verfassungsprinzip III).

## Verzeichnis

| Tier | Topic | Schema-Datei |
|---|---|---|
| `hot`   | `hot.killswitch.trip.v1`              | `hot/killswitch-trip.v1.avsc` |
| `warm`  | `warm.dropcopy.<venue>.v1`            | `warm/dropcopy.v1.avsc` |
| `warm`  | `warm.recon.mismatch.v1`              | `warm/recon-mismatch.v1.avsc` |
| `warm`  | `warm.entitlements.limit-update.v1`   | `warm/entitlements-limit-update.v1.avsc` |
| `cold`  | `cold.oms.event.v1`                   | `cold/oms-event.v1.avsc` |
| `cold`  | `cold.exec.fill.v1`                   | `cold/exec-fill.v1.avsc` |
| `cold`  | `cold.marketdata.l1.v1`               | `cold/marketdata-l1.v1.avsc` |
| `cold`  | `cold.surveillance.alert.v1`          | `cold/surveillance-alert.v1.avsc` |
| `cold`  | `cold.reporting.rts22-submission.v1`  | `cold/reporting-rts22.v1.avsc` |
| `audit` | `audit.command.v1`                    | `audit/audit-command.v1.avsc` |
| `tca`   | `tca.event.v1`                        | `tca/tca-event.v1.avsc` |
| `region`| `region.handover.cutover.v1`          | `region/handover-cutover.v1.avsc` |

Phase 2 liefert die zentralen Schemas (oms-event, exec-fill, audit-command,
killswitch-trip); übrige Topics werden in den jeweiligen User-Story-Phasen
angelegt.
