plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — Pre-trade risk gateway. Hot-path Disruptor evaluator (US7 / FR-005c)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.aeron:aeron-all:1.45.0")
    implementation("org.agrona:agrona:1.21.2")
    implementation("com.lmax:disruptor:4.0.0")
    implementation("org.springframework.kafka:spring-kafka:3.2.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0") // entitlement-cache JSON
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:pretrade-risk"))
    implementation(project(":libs:aeron-transport"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:audit-chain"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
