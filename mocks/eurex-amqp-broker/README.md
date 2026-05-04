# Eurex AMQP Broker Mock

Apache Qpid Broker-J 9.2 als Container, simuliert die Eurex Clearing
AMQP-1.0-Endpunkte. Wird per `compose.dev.yaml` Service `qpid-broker`
gestartet.

| Queue | Inhalt |
|---|---|
| `eurex.tradecapture` | Inbound TradeCaptureReports (FIXML 5.0 SP2) |
| `eurex.position`     | PositionMaintenanceRequest |
| `eurex.broadcasts`   | Public Broadcasts |
| `eurex.cre.outbox`   | Common Report Engine Daily Reports (Pull via SFTP, dieser Queue ist eine Vereinfachung für lokales Testing) |

## Sample-Payloads

`mocks/eurex-amqp-broker/samples/` enthält FIXML-Stubs, die per
`docker exec qpid-broker java -jar samples-loader.jar` in die Queues
geladen werden können.

## Konfiguration

Default-Setup liegt im Image. Override via Volume-Mount auf
`/usr/local/qpid/etc/`.
