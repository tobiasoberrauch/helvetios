pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "swiss-tms-platform"

// Phase 2 libs
include("libs:domain-model")
include("libs:time-sync")
include("libs:audit-chain")
include("libs:observability")
include("libs:security")
include("libs:fix-codec")
include("libs:sbe-codec")
include("libs:aeron-transport")
include("libs:kafka-transport")
include("libs:fixml-codec")
include("libs:fpml-codec")
include("libs:pretrade-risk")

include("apps:oms-service")
include("apps:venue-adapter-six")
include("apps:reconciler-service")
include("tests:architecture")

include("apps:clearing-adapter-eurex")
include("apps:reporting-service")
include("apps:entitlements-service")
include("apps:market-data-service")
include("apps:venue-adapter-bloomberg")
include("apps:venue-adapter-refinitiv")
include("apps:venue-adapter-tradeweb")
include("apps:venue-adapter-marketaxess")
include("apps:venue-adapter-bidfx")
include("apps:pretrade-risk-gateway")
include("apps:inbound-fix-acceptor")
include("apps:ems-service")
include("apps:region-router")
include("apps:clearing-adapter-otcc")
include("apps:clearing-adapter-six")
include("apps:venue-adapter-eurex")
include("apps:audit-service")
include("apps:position-keeping")
