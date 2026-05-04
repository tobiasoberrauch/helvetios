# C4 Level 3 — Components

Component-level diagrams are added in **Phase 12 (US10 — Portfolio walkthrough)** for the three services that show up in the 30-minute walkthrough:

- `apps/oms-service/` — application services, repositories, ports.
- `apps/ems-service/` — matching engine, SOR, algo strategies, Aeron Cluster bootstrap.
- `apps/venue-adapter-six/` — STI / OTI / QTI / IMI / MDDX / TRI sub-modules behind the single `VenueGatewayPort`.

Until Phase 12, refer to the Container diagram in [`containers.md`](./containers.md) and the package layout in `apps/<service>/src/main/java/`.
