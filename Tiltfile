# Swiss Trading & Market Support Platform — Tilt inner-loop config.
#
# Tilt orchestrates the local Kubernetes (kind / k3d) deployment of every
# service and mock. Phase 1 ships only the bare scaffold; per-service
# resources are wired in tools/tilt/extensions/*.star as each phase
# brings a service online (see specs/001-swiss-tms-platform/tasks.md).

load_dynamic("tools/tilt/extensions/oms.star")              # Phase 3 (US1)
load_dynamic("tools/tilt/extensions/venue_adapter_six.star") # Phase 3 (US1)
load_dynamic("tools/tilt/extensions/six_mts_stub.star")     # Phase 3 (US1)
load_dynamic("tools/tilt/extensions/trader_ui.star")        # Phase 3B

# Local infrastructure (Postgres, Kafka, Apicurio, Redis, Keycloak, OpenBao,
# Grafana, Prometheus, Tempo, OpenSearch, FIXimulator, Qpid Broker-J, ...)
# is brought up via Docker Compose alongside Tilt to avoid forcing Helm onto
# everything in dev mode.
docker_compose("./compose.dev.yaml")

# Tilt UI button to open the Grafana dashboard once the stack is up.
update_settings(suppress_unused_image_warnings=ALL)

print("""
Swiss-TMS-Platform — Tilt configuration loaded.

  Phase 1 (Setup):       only the docker-compose ancillary services start.
  Phase 3 (US1) onwards: oms-service, venue-adapter-six, six-mts-stub, trader-ui.

Run: tilt up    Open: http://localhost:10350
""")
