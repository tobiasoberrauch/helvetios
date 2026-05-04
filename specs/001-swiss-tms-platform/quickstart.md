# Quickstart

This guide takes a fresh developer machine to a working **end-to-end order roundtrip** against the SIX mock venue in under ten minutes. Everything below runs locally without any cloud account, vendor account, or external network beyond a public package registry.

If you've ten minutes — go to [Five-step happy path](#five-step-happy-path).

## Prerequisites

| Tool | Version | Why |
|---|---|---|
| Git | ≥ 2.43 | Clone |
| JDK | 21 (Temurin or Microsoft) | Service-plane + hot path |
| Python | 3.12 | Reference data, surveillance, fixtures |
| Node.js | 20 LTS | Trader UI |
| Go | 1.22 | PTP audit reporter, conformance harness drivers |
| Docker | Desktop / Colima / Rancher Desktop with ≥ 8 CPU cores and ≥ 16 GB RAM | Local containers |
| `kubectl` | ≥ 1.30 | Kubernetes CLI |
| `kind` | ≥ 0.24 (or `k3d` ≥ 5.7) | Local Kubernetes |
| `helm` | ≥ 3.15 | K8s app composition |
| `helmfile` | ≥ 0.166 | Per-environment composition |
| `tilt` | ≥ 0.33 | Inner-loop dev UI |
| `uv` | ≥ 0.4 | Python workspace |
| `make` | any | Top-level driver |

Optional (for full-stack work):

| Tool | Version | Why |
|---|---|---|
| Cargo / `rustup` | latest stable | If you touch the (optional) Rust workspace |
| `terraform` | ≥ 1.9 | Cloud / prod-shadow infra |
| `ansible` | ≥ 9 | Bare-metal trading-floor playbooks |
| `cosign` | ≥ 2 | Sigstore signing in CI |

The Gradle wrapper is bundled (`./gradlew`); no system Gradle install is required.

## Five-step happy path

Run from the repo root once cloned:

```bash
git clone https://github.com/tobiasoberrauch/swiss-tms-platform.git
cd swiss-tms-platform
make scaffold      # generates SBE codecs, JAXB classes, Avro classes, downloads vendor mirrors
tilt up            # boots ~17 services + mocks; opens http://localhost:10350
make smoke         # end-to-end: submits an order, asserts ExecutionReport, checks recon
```

When `make smoke` exits 0, you have a working order roundtrip. The trader UI is available at http://localhost:5173. The Grafana dashboards are at http://localhost:3000 (`admin` / `tilt-admin`).

To stop everything: `Ctrl-C` in the `tilt up` window, then `tilt down`.

## What `tilt up` brings up

| Service | Port | Notes |
|---|---|---|
| `oms-service` | 8080 (REST) / 9090 (gRPC) | OMS aggregate |
| `inbound-fix-acceptor` | 9001–9020 | One acceptor port per test client |
| `pretrade-risk-gateway` | (Aeron IPC only) | Co-located with acceptor |
| `ems-service` | 9100 | EMS / Aeron Cluster (3-node local) |
| `market-data-service` | 9200 | Market-data normaliser |
| `reference-data-service` | 8081 | Python/FastAPI |
| `entitlements-service` | 8082 | DACS / EMRS sync (mocked) |
| `reporting-service` | 8083 | Spring Batch jobs |
| `surveillance-service` | 8084 | Flink job (local mini-cluster) |
| `audit-service` | 8085 | Hash-chained audit log writer |
| `reconciler-service` | 8086 | Kafka Streams |
| `region-router` | 8087 | Single-region in local-dev |
| `clearing-adapter-eurex` | 8088 | Talks to Apache Qpid Broker-J mock |
| `clearing-adapter-six` | 8089 | SECOM ISO 20022 mock |
| `venue-adapter-six` | 8101 | Talks to `mocks/six-mts-stub/` |
| `venue-adapter-eurex` | 8102 | Talks to `mocks/eurex-amqp-broker/` |
| `venue-adapter-bloomberg` | 8103 | Talks to `mocks/bloomberg-stub/` |
| `venue-adapter-refinitiv` | 8104 | Talks to `mocks/refinitiv-ema-provider/` |
| `venue-adapter-tradeweb` | 8105 | Talks to FIXimulator (Tradeweb dialect) |
| `venue-adapter-marketaxess` | 8106 | Talks to FIXimulator (MarketAxess dialect) |
| `venue-adapter-bidfx` | 8107 | Mock BidFX SDK |
| `venue-adapter-cfets` | 8108 | Python proxy adapter |
| `trader-ui` | 5173 | React + Perspective + Vite |
| Postgres 16 | 5432 | OMS + reference-data + audit |
| Kafka 3.7 (KRaft) | 9092 | Single broker locally |
| Apicurio Registry | 8090 | Avro schemas |
| Redis 7 | 6379 | Hot state |
| QuestDB 9 | 9000 | Tick hot tier |
| ClickHouse 24 | 8123 | Tick warm tier (limited locally) |
| OpenSearch 2 | 9200 | FIX archive |
| OpenBao | 8200 | Secrets / PKI / Transit |
| Keycloak 25 | 8180 | OIDC / OAuth2 |
| Grafana | 3000 | Dashboards (provisioned via Helm) |
| Prometheus | 9091 | Metrics |
| Tempo | 3200 | Traces |
| FIXimulator | 9876–9890 | One port per venue-adapter outbound session |
| Apache Qpid Broker-J | 5672 | Eurex AMQP mock |
| `chrony` (NTP) | 123/udp | Time sync (PTP simulator runs alongside) |
| `mocks/ptp-grandmaster-sim/` | (PTP) | Software PTP master |

Tilt's `localhost:10350` UI shows the live status of every container and a per-service log tail.

## Submit a test order via curl

```bash
TOKEN=$(curl -s http://localhost:8180/realms/swiss-tms/protocol/openid-connect/token \
  -d 'grant_type=password' \
  -d 'client_id=trader-ui' \
  -d 'username=alice.trader' \
  -d 'password=demo' | jq -r .access_token)

curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d @- <<'EOF'
{
  "clOrdId": "ALICE-DEMO-001",
  "instrumentId": { "isin": "CH0012005267", "mic": "XSWX" },
  "side": "BUY",
  "ordType": "LIMIT",
  "quantity": 100,
  "price": 99.50,
  "timeInForce": "DAY",
  "routingMode": "DMA",
  "preferredVenue": "XSWX"
}
EOF
```

Response: `202 Accepted` with the assigned `orderId`. The trader UI updates within a second; the SIX mock generates a partial fill followed by a full fill; the reconciler reconciles the drop-copy.

## Submit a test order via FIX (sell-side flow)

A simulated client is bundled in `mocks/test-client/`. Run:

```bash
make demo-client SENDER=ACME-CAPITAL TARGET=SWISSTMS PORT=9001
```

This logs on a FIX session (FIXT.1.1 / FIX 5.0 SP2), authenticates with the demo mTLS cert from `mocks/test-client/certs/`, sends a `NewOrderSingle (35=D)`, and prints the resulting `ExecutionReport (35=8)` chain.

## Run the property tests

```bash
make test-property         # jqwik (Java) + Hypothesis (Python) suites
```

Highlights:
- `FixCodecPropertyTest` — random FIX 4.4 / 5.0 SP2 messages roundtrip via QuickFIX/J.
- `OrderStateMachinePropertyTest` — invariant: after `Filled` only `TRADE_BUSTED` is legal.
- `SbeOrdersRoundtripTest` — random `OrderSubmit` / `OrderAck` / `RiskRejection` SBE roundtrip.
- `FpmlInterestRateStreamTest` — random FpML 5.12 IRS stream XSD-validated.
- `FixmlEurexTradeCaptureTest` — random Eurex C7 trade-capture XSD-validated.
- `RtsTwoTwoXmlTest` — random RTS-22 XML validated against the ESMA XSD.

## Run the conformance suite (no external venue)

```bash
make test-conformance      # FIXimulator + custom mocks per venue
```

The conformance suite executes per-venue end-to-end test scenarios (NewOrder → PartialFill → Cancel → Reject etc.) against the bundled mocks.

## View Grafana

http://localhost:3000 (`admin` / `tilt-admin`).

Provisioned dashboards (under "Swiss TMS"):

- **Tick-to-trade latency** — histogram of hot-path latency across all venues.
- **FIX session health** — per-session up/down, gap fills, last sequence numbers.
- **Kafka lag** — per consumer group.
- **Eurex AMQP throughput** — clearing pipeline.
- **Region failover** — per-region health and follow-the-sun handover signals.
- **Pre-trade risk decisions** — approval / rejection rate by reason code.

## Add a new venue adapter

```bash
make new-venue NAME=cboe   # scaffolds apps/venue-adapter-cboe/
```

The scaffold generator creates:

- `apps/venue-adapter-cboe/build.gradle.kts`
- `apps/venue-adapter-cboe/src/main/java/ch/swisstms/venue/cboe/CboeVenueAdapter.java` implementing `VenueGatewayPort`
- `apps/venue-adapter-cboe/src/main/resources/application.yml`
- A skeleton conformance test
- A skeleton ADR `docs/decisions/0xxx-venue-adapter-cboe.md`

Implement the port methods and a Pact contract test; the rest of the platform is unchanged.

## Run the chaos suite locally

```bash
make test-chaos            # requires kind (Tilt is using it) and Chaos Mesh installed
```

The chaos suite runs Chaos Mesh manifests under `tests/chaos/` and verifies that:
- A FIX session-drop triggers automatic resync with no lost or duplicate fills.
- A Kafka-broker isolation triggers ISR-shrink and consumer back-pressure handling.
- A PTP skew injection raises an alert and the platform refuses new orders during the skew.
- An Aeron Cluster leader-kill triggers a Raft re-election and the EMS resumes.
- An AMQP broker-restart triggers `CachingConnectionFactory` reconnect with no message loss.

## Bring up the production-shadow environment in AKS (per region)

```bash
cd infra/terraform/environments/prod-shadow-zh
terraform init
terraform plan -out tfplan
terraform apply tfplan        # provisions AKS, networking, observability, Kafka, Postgres
cd ../../../helmfile
helmfile -e prod-shadow-zh sync
```

Repeat for `prod-shadow-ld4`, `prod-shadow-ny4`, `prod-shadow-ty3`. The four environments form one active-active deployment.

Onboarding for vendor sandboxes (Bloomberg B-PIPE, Refinitiv RDP, Eurex Clearing UAT) is documented in `ops/runbooks/vendor-onboarding.md`.

## Common failures

| Symptom | Cause | Fix |
|---|---|---|
| `tilt up` exits with "context deadline exceeded" on Postgres init | Docker Desktop has < 16 GB RAM allocated | Increase Docker memory; restart Docker Desktop |
| Kafka topic schemas reject reads with "incompatible schema" | Apicurio compatibility mode set wrong | `make schema-reset` (dev only) |
| FIXimulator refuses logon | Local clock skew > 30s | `make ntp-sync` (forces `chrony` resync) |
| Tilt hot-reload churn for one service | Gradle continuous build looping | `tilt disable <service>`, then `tilt enable <service>` |
| `make smoke` fails at recon step | Drop-copy publisher not yet up | Wait 10 seconds, rerun (or `tilt status` to verify all green) |
| `make test-property` times out on `FpmlInterestRateStreamTest` | First run downloads FpML 5.12 XSD bundle | Re-run; subsequent runs cached |
| Bloomberg adapter says "BLPAPI not licensed" | The vendor mirror in `infra/maven-mirror/` was not pulled | `make vendor-mirror`; verify `BLPAPI_HOME` is set in `.env` |

## Where to go next

- New to the codebase? Open `docs/interview/30min-walkthrough.md` and follow it.
- Looking for a specific subsystem? Start at `docs/architecture/containers.md` (the C4 container diagram) and click through.
- Hitting a regulator-related question? `docs/decisions/` (ADRs) and `ops/runbooks/` answer most of them.
- Curious about hot-path latency? `tests/performance/jmh/` is where the microbenchmarks live; `ops/grafana/dashboards/tick-to-trade-latency.json` is the live view.
