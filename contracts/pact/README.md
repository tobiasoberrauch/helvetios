# Pact Consumer-Driven Contracts

Generated `*.pact.json` Dateien werden hier von Konsumenten abgelegt und
von Providern in CI verifiziert.

## Phase 3 (US1)

- `trader-ui-oms.pact.json` — Konsument: `apps/trader-ui/`, Provider: `apps/oms-service/`.

## Phase 5 (US3)

- `oms-venueGatewayPort.pact.json` — Konsument: `apps/oms-service/`, Provider: jeder `apps/venue-adapter-*/`.

## Phase 6 (US4)

- `oms-clearingPort.pact.json` — Konsument: `apps/oms-service/`, Provider: `apps/clearing-adapter-eurex/`.

## Phase 8 (US6)

- `marketdata-vendorAdapterPort.pact.json` — Konsument: `apps/market-data-service/`, Provider: `apps/venue-adapter-refinitiv/` und `apps/venue-adapter-bloomberg/`.

## Phase 13

- `inboundFixAcceptor-pretradeRiskPort.pact.json` — Konsument: `apps/inbound-fix-acceptor/`, Provider: `apps/pretrade-risk-gateway/`.

Pact-Files werden NICHT von Hand gepflegt — sie entstehen aus den
Consumer-Tests und werden vom CI-Stage `unit` produziert + an einen
Pact-Broker (oder direkt ins Repo unter `contracts/pact/`) gepushed.
