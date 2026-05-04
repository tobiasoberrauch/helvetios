# mocks/six-tr-submission

T143 — Local SFTP listener that stands in for the SIX Trade Repository inbound endpoint.

- Service name in `compose.dev.yaml`: `six-tr-submission`
- Port: `2222 → 22`
- User / password: `swisstms / swisstms-dev`
- Drop directory: `/home/swisstms/upload`

The reporting-service `FinfraGArt39Job` writes one TRI-XML file per business day to this drop.
The integration test `DailyReportingBatchTest` (T131) verifies the file lands and that the
SHA-256 hash matches the audit-chain entry tagged `reporting.finfrag.batch.completed`.
