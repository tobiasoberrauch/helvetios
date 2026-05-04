# Glossary

A reference for the FIX, Trading, Clearing, Reporting, and Time-Sync terminology used in this repository.

## A

- **Aeron** — Open-source low-latency messaging library by Real Logic. Used for hot-path IPC and cluster replication.
- **Aeron Cluster** — Raft-based replication built on top of Aeron. Per-region in this platform.
- **AiEX** — Tradeweb's automated execution rule engine for RFQ flows.
- **Algo Wheel** — A sell-side mechanism that automatically selects the best algorithm/venue for a given order.
- **AMQP 1.0** — Advanced Message Queuing Protocol, version 1.0. Used by Eurex Clearing for FIXML transport.
- **APA** — Approved Publication Arrangement. MarketAxess Trax APA is one such arrangement under MiFID-II.
- **ARM** — Approved Reporting Mechanism (MiFID-II RTS-22). LSEG TRADEcho is the dominant European ARM.

## B

- **B-PIPE** — Bloomberg's enterprise market-data feed (vs the Desktop API).
- **BLPAPI** — Bloomberg API. The proprietary library used for `//blp/refdata`, `//blp/mktdata`, `//blp/emapisvc`.

## C

- **C7** — Eurex's clearing platform (post-trade), distinct from T7 (trading).
- **CCP** — Central Counterparty. Novates trades and stands between buyer and seller. Eurex Clearing and SIX x-clear are CCPs.
- **CFETS** — China Foreign Exchange Trade System. Northbound access via Bloomberg / Tradeweb / MarketAxess.
- **CFI Code** — Classification of Financial Instruments (ISO 10962).
- **ClOrdID** — FIX Tag 11. Client-supplied unique order identifier.
- **CRE** — Eurex Common Report Engine. Daily SFTP-pulled clearing reports.

## D

- **DACS** — Refinitiv Data Access Control System. Per-record permissioning via PE-codes.
- **DMA** — Direct Market Access. Pass-through routing without sell-side intervention.
- **Drop-Copy** — Independent copy of a venue's executions, treated as the source of truth in this platform (Constitution Principle V).

## E

- **EMIR** — European Market Infrastructure Regulation. Mandates derivatives reporting to a Trade Repository.
- **EMRS** — Bloomberg's Entitlement Management and Reporting System.
- **EMSX-API** — Bloomberg Execution Management System API. `//blp/emapisvc` (prod) / `//blp/emapisvc_beta` (UAT).
- **ETI** — Eurex Trader Interface (the binary T7 protocol).
- **ExecType** — FIX Tag 150. Lifecycle event type on an ExecutionReport.

## F

- **FIA-EPTA** — FIA European Principal Traders Association. Publishes market-access framework guidance.
- **FIX** — Financial Information eXchange protocol. The dominant electronic trading message format.
- **FIXML** — XML-based FIX dialect used for post-trade messages (notably Eurex C7).
- **FinfraG** — Swiss Financial Market Infrastructure Act. Article 39 mandates transaction reporting to the SIX Trade Repository.
- **FINMA** — Swiss Financial Market Supervisory Authority.
- **FpML** — Financial products Markup Language. XML for OTC derivatives confirmations.
- **Funari** — A SIX-specific order type (Fill-and-store-the-rest).

## H

- **Hexagonal Architecture** — Ports-and-adapters. The platform's central architectural commitment (Constitution Principle I).
- **HandlInst** — FIX Tag 21. Determines DMA / algo-wheel / care-order handling.

## I

- **iLink-3** — CME's binary order entry protocol; supported by Artio.
- **IMI** — SIX's ITCH-based market-data feed.
- **ITCH** — A binary market-data dissemination protocol family.

## J

- **JdbcStoreFactory** — QuickFIX/J's JDBC-backed FIX session-state store. Used in this platform with row-level Postgres locking.
- **jqwik** — Java property-based testing library; mandatory under Constitution Principle VII.

## K

- **kdb+ / q** — Tier-1 commercial time-series database. Documented as drop-in for QuestDB+ClickHouse.
- **Kill-Switch** — Mechanism to immediately cancel all orders for a defined scope (trader / strategy / desk / client).
- **KRaft** — Kafka Raft Metadata mode (replaces ZooKeeper).

## L

- **LEI** — Legal Entity Identifier (ISO 17442). Required on RTS-22 / EMIR / FinfraG reports.
- **Linkerd** — Rust-based service mesh used in this platform for in-mesh mTLS.

## M

- **MADR** — Markdown Architectural Decision Records. Used for `docs/decisions/`.
- **MIC** — Market Identifier Code (ISO 10383). E.g., `XSWX` (SIX), `XEUR` (Eurex).
- **MoldUDP64** — UDP-based market-data dissemination format used by SIX IMI.

## O

- **OMS** — Order Management System.
- **ODPS** — Refinitiv's Open DACS Permission Server (HTTP REST).
- **OUCH** — A binary order-entry protocol family.
- **Outbox** — Database pattern for reliable event publishing (Postgres → Debezium → Kafka).

## P

- **Pact** — Consumer-driven contract testing tool.
- **PE-Code** — Refinitiv DACS Product Permission code.
- **PHC** — PTP Hardware Clock — the hardware time source on a NIC.
- **Pixie** — BidFX's firm-tradable-quote protocol.
- **PossDupFlag** — FIX Tag 43. Indicates a possibly-duplicate replay.
- **PTP** — Precision Time Protocol (IEEE 1588). Sub-microsecond clock synchronisation.
- **Puffin** — BidFX's shared-streaming protocol.

## R

- **RDP** — Refinitiv Data Platform. REST + WebSocket V2 API.
- **RFQ** — Request For Quote. Multi-dealer auction model used in fixed-income and FX.
- **RTS-6/7** — MiFID-II Algorithmic Trading Governance regulatory technical standards.
- **RTS-22** — MiFID-II Transaction Reporting RTS.
- **RTS-24** — MiFID-II Order Record Keeping RTS.
- **RTS-25** — MiFID-II Time Synchronisation RTS.
- **RTS-27/28** — MiFID-II Best Execution RTS (RTS-27 suspended in EU; UK/CH still expect).
- **RTSDK** — Refinitiv Real-Time SDK (formerly Elektron). Includes EMA (high-level) and ETA (low-level).

## S

- **SBE** — Simple Binary Encoding. Used internally for hot-path messages.
- **SECOM** — SIX's settlement protocol (ISO 20022 sese.023, sese.025).
- **SOR** — Smart Order Router.
- **SoupBinTCP** — A binary session-layer protocol used with OUCH and ITCH.
- **SPIFFE / SPIRE** — Workload identity framework. Used for in-mesh mTLS.

## T

- **T7** — Eurex's trading platform (binary ETI; FIX gateway as fallback).
- **TCA** — Transaction Cost Analysis. Required evidence for RTS-28 best-execution reports.
- **TIF** — Time In Force (DAY / IOC / FOK / GTC / GTD / OPG).
- **Trax APA** — MarketAxess Trax Approved Publication Arrangement.

## V

- **VWAP / TWAP / POV / IS** — Volume-Weighted Average Price / Time-Weighted Average Price / Percent of Volume / Implementation Shortfall — common execution algorithms.

## W

- **WORM** — Write-Once-Read-Many storage. S3 Object Lock satisfies it for regulatory archival.
- **W3C TraceContext** — `traceparent` propagation. We carry it through FIX via custom Tag 7777.

## X

- **x-clear** — SIX's central counterparty for Swiss equities.
