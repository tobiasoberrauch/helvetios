/*
 * Swiss Trading & Market Support Platform — Structurizr DSL workspace.
 *
 * This is the single source of truth for C4 architecture diagrams. Run
 * Structurizr Lite locally (or upload to structurizr.com) to render
 * Context (Level 1), Container (Level 2), and Component (Level 3) views.
 *
 * Component-level diagrams (Level 3) are added in Phase 12 (US10) for
 * the portfolio walkthrough; this Phase-1 file ships only Context and
 * Container so the build site has something to render from day one.
 */

workspace "Swiss Trading & Market Support Platform" "Reference mono-repo for a Swiss bank in Basel" {

    !identifiers hierarchical

    model {
        # ---------------------------------------------------------------
        # External actors
        # ---------------------------------------------------------------
        trader        = person "Trader / Portfolio Manager"
        compliance    = person "Compliance Officer"
        ops           = person "Market Support / IT-Ops"
        client        = person "Buy-side client (FIX-as-server)"
        finma         = softwareSystem "FINMA / SIX Trade Repository" "Swiss regulator + trade repository" "External"
        arm           = softwareSystem "LSEG TRADEcho ARM" "Approved Reporting Mechanism for RTS-22" "External"
        emir_tr       = softwareSystem "DTCC GTR + REGIS-TR" "EMIR trade repositories" "External"

        # ---------------------------------------------------------------
        # External venues / vendors
        # ---------------------------------------------------------------
        six           = softwareSystem "SIX Swiss Exchange" "OTI / STI / QTI / IMI / MDDX" "External"
        eurex         = softwareSystem "Eurex T7 + C7 Clearing" "T7 ETI binary + FIXML over AMQP 1.0" "External"
        bloomberg     = softwareSystem "Bloomberg" "BLPAPI / EMSX / B-PIPE / Data License" "External"
        refinitiv     = softwareSystem "LSEG Refinitiv" "RTSDK / RDP / DACS" "External"
        tradeweb      = softwareSystem "Tradeweb" "TradeXpress + AiEX" "External"
        marketaxess   = softwareSystem "MarketAxess" "Open Trading + Composite+ + Trax APA" "External"
        bidfx         = softwareSystem "BidFX" "Pixie + Puffin" "External"
        cfets         = softwareSystem "CFETS" "Bond Connect / Swap Connect via proxy" "External"
        xclear        = softwareSystem "SIX x-clear / SECOM" "Swiss CSD" "External"

        # ---------------------------------------------------------------
        # The platform
        # ---------------------------------------------------------------
        platform = softwareSystem "Swiss Trading & Market Support Platform" "Reference mono-repo" {
            tags "internal"

            # DMZ
            inboundFix       = container "inbound-fix-acceptor" "Artio + QuickFIX/J" "Java 21"
            pretradeRisk     = container "pretrade-risk-gateway" "Aeron IPC + Disruptor" "Java 21"
            regionRouter     = container "region-router" "Spring Boot" "Java 21"

            # Internal core
            oms              = container "oms-service" "Spring Boot 3 + Spring Statemachine" "Java 21"
            ems              = container "ems-service" "Aeron Cluster (Raft) + Disruptor + Artio" "Java 21"
            marketData       = container "market-data-service" "Aeron multicast" "Java 21"
            referenceData    = container "reference-data-service" "FastAPI" "Python 3.12"
            entitlements     = container "entitlements-service" "Spring Boot" "Java 21"
            auditSvc         = container "audit-service" "Hash-chain writer" "Java 21"
            reconciler       = container "reconciler-service" "Kafka Streams" "Java 21"
            traderUi         = container "trader-ui" "React + FINOS Perspective" "TypeScript"

            # Async / compliance
            reporting        = container "reporting-service" "Spring Batch" "Java 21"
            surveillance     = container "surveillance-service" "Apache Flink" "Python 3.12"
            positionKeeping  = container "position-keeping" "Kafka consumer" "Java 21"

            # Adapters (DMZ — venue connectivity)
            venueSix         = container "venue-adapter-six" "FIX 4.4 + OUCH + ITCH + Artio" "Java 21"
            venueEurex       = container "venue-adapter-eurex" "T7 ETI binary + FIX gateway" "Java 21"
            venueBloomberg   = container "venue-adapter-bloomberg" "BLPAPI v3" "Java 21"
            venueRefinitiv   = container "venue-adapter-refinitiv" "EMA RTSDK" "Java 21"
            venueTradeweb    = container "venue-adapter-tradeweb" "QuickFIX/J TradeXpress" "Java 21"
            venueMarketaxess = container "venue-adapter-marketaxess" "QuickFIX/J + Trax APA" "Java 21"
            venueBidfx       = container "venue-adapter-bidfx" "Pixie + Puffin SDK" "Java 21"
            venueCfets       = container "venue-adapter-cfets" "Proxy via Tradeweb / Bloomberg" "Python 3.12"
            clearingEurex    = container "clearing-adapter-eurex" "Apache Qpid JMS (AMQP 1.0)" "Java 21"
            clearingSix      = container "clearing-adapter-six" "ISO 20022 SECOM" "Java 21"
            clearingOtcc     = container "clearing-adapter-otcc" "OTCC ↔ SHCH" "Java 21"

            # Persistence
            postgres         = container "PostgreSQL 16" "OLTP + Outbox + event store" "Aurora Global DB" "Database"
            kafka            = container "Kafka 3.7 (KRaft)" "Warm + cold event spine" "" "Database"
            questdb          = container "QuestDB 9" "Tick hot tier" "" "Database"
            clickhouse       = container "ClickHouse 24" "Tick warm tier" "" "Database"
            redis            = container "Redis 7" "Hot state cache" "" "Database"
            s3               = container "S3 / MinIO Object Lock" "WORM archive" "" "Database"

            # Aeron internal IPC bus
            aeron            = container "Aeron IPC + Cluster + Archive" "Hot-path messaging" "" "Bus"
        }

        # ---------------------------------------------------------------
        # Relationships
        # ---------------------------------------------------------------
        client      -> platform.inboundFix       "Submits orders via FIX" "FIX 4.4 / 5.0 SP2"
        trader      -> platform.traderUi         "Submits orders / monitors execution"
        compliance  -> platform.surveillance     "Reviews market-abuse alerts"
        compliance  -> platform.reporting        "Triggers / reviews regulatory reports"
        ops         -> platform.entitlements     "Manages entitlements + kill-switch"
        ops         -> platform.auditSvc         "Inspects audit chain"

        platform.inboundFix     -> platform.pretradeRisk    "Hot-path handoff (Aeron IPC)"
        platform.pretradeRisk   -> platform.regionRouter    "Cleared orders"
        platform.regionRouter   -> platform.oms             "Region-tagged orders"
        platform.oms            -> platform.ems             "Order routing"
        platform.ems            -> platform.aeron           "Match / SOR / algos"
        platform.aeron          -> platform.venueSix        "Outbound (hot)"
        platform.aeron          -> platform.venueEurex      "Outbound (hot)"
        platform.ems            -> platform.venueTradeweb   "Outbound (warm)"
        platform.ems            -> platform.venueMarketaxess "Outbound (warm)"
        platform.ems            -> platform.venueBidfx      "Outbound (warm/hot)"
        platform.ems            -> platform.venueBloomberg  "EMSX submission"
        platform.ems            -> platform.venueCfets      "Asia routing"

        platform.venueSix       -> platform.kafka           "Drop-copy → warm.dropcopy.six.v1"
        platform.venueEurex     -> platform.kafka           "Drop-copy"
        platform.venueTradeweb  -> platform.kafka           "Drop-copy"
        platform.venueMarketaxess -> platform.kafka         "Drop-copy"
        platform.venueBidfx     -> platform.kafka           "Drop-copy"
        platform.venueBloomberg -> platform.kafka           "Drop-copy (EMSX)"

        platform.kafka          -> platform.reconciler      "warm.dropcopy.* + cold.oms.event.v1"
        platform.reconciler     -> platform.kafka           "cold.exec.fill.v1 (authoritative)"
        platform.kafka          -> platform.surveillance    "cold.exec.fill.v1 + cold.book.event.v1"
        platform.kafka          -> platform.reporting       "cold.exec.fill.v1"
        platform.kafka          -> platform.positionKeeping "cold.exec.fill.v1"

        platform.oms            -> platform.postgres        "Order state + Outbox"
        platform.marketData     -> platform.questdb         "Tick hot tier"
        platform.questdb        -> platform.clickhouse      "Roll-up to warm tier"
        platform.ems            -> platform.redis           "Session cache + intraday state"
        platform.aeron          -> platform.s3              "Aeron Archive WORM mirror"
        platform.auditSvc       -> platform.s3              "Audit chain WORM"
        platform.reporting      -> platform.s3              "Regulator submissions WORM"

        platform.reporting      -> arm                      "RTS-22 over HTTPS"
        platform.reporting      -> finma                    "FinfraG Art. 39 over SFTP"
        platform.reporting      -> emir_tr                  "EMIR over HTTPS"
        platform.reporting      -> marketaxess              "Trax APA over FIX"

        platform.venueSix       -> six                      "STI / OTI / QTI / IMI / MDDX / TRI"
        platform.venueEurex     -> eurex                    "T7 ETI"
        platform.venueBloomberg -> bloomberg                "BLPAPI / EMSX"
        platform.venueRefinitiv -> refinitiv                "RTSDK / RDP / DACS"
        platform.venueTradeweb  -> tradeweb                 "TradeXpress FIX"
        platform.venueMarketaxess -> marketaxess            "Open Trading + Trax APA"
        platform.venueBidfx     -> bidfx                    "Pixie + Puffin"
        platform.venueCfets     -> cfets                    "Bond Connect / Swap Connect"
        platform.clearingEurex  -> eurex                    "FIXML over AMQP 1.0"
        platform.clearingSix    -> xclear                   "ISO 20022 SECOM"
    }

    views {
        systemContext platform "Context" {
            include *
            autolayout lr
        }

        container platform "Containers" {
            include *
            autolayout
        }

        styles {
            element "Person" {
                shape person
                background #4B7BEC
                color #ffffff
            }
            element "External" {
                background #95a5a6
                color #ffffff
            }
            element "Database" {
                shape cylinder
                background #2c3e50
                color #ffffff
            }
            element "Bus" {
                shape pipe
                background #16a085
                color #ffffff
            }
            element "internal" {
                background #2c3e50
                color #ffffff
            }
        }

        theme default
    }
}
