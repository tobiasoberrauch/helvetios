plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — MarketAxess Open Trading + Composite+ + Trax APA adapter (US7)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka:3.2.4")
    implementation("org.quickfixj:quickfixj-core:2.3.2")
    implementation("org.quickfixj:quickfixj-messages-fixt11:2.3.2")
    implementation("org.quickfixj:quickfixj-messages-fix50sp2:2.3.2")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:fix-codec"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

// Scaffold module — no @SpringBootApplication yet. Phase wiring will introduce it; until then
// disable bootJar and re-enable plain jar so the build is green.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { enabled = false }
tasks.named<Jar>("jar") { enabled = true }
