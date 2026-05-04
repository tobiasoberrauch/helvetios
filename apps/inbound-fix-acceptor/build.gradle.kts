plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — Inbound FIX-as-server (Artio + QuickFIX/J fallback) — Phase 13."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.quickfixj:quickfixj-core:2.3.2")
    implementation("org.quickfixj:quickfixj-messages-fix44:2.3.2")
    implementation("org.quickfixj:quickfixj-messages-fix50sp2:2.3.2")
    // Artio — the high-throughput path. Vendored from Real Logic; pull via
    // infra/maven-mirror/ in Phase 14 prod-shadow.
    // implementation("uk.co.real-logic:artio-core:0.155")
    implementation("io.aeron:aeron-all:1.45.0")
    implementation("org.yaml:snakeyaml:2.3") // ClientSessionLoader

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:fix-codec"))
    implementation(project(":libs:aeron-transport"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:audit-chain"))
    implementation(project(":libs:observability"))
    implementation(project(":libs:pretrade-risk"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
