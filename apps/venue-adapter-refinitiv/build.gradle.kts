plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — Refinitiv EMA RTSDK adapter (US6)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Refinitiv EMA — pulled from infra/maven-mirror/ in Phase 14.
    // implementation("com.refinitiv.ema:ema:3.7.x")

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

// Scaffold module — no @SpringBootApplication yet. Phase wiring will introduce it; until then
// disable bootJar and re-enable plain jar so the build is green.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { enabled = false }
tasks.named<Jar>("jar") { enabled = true }
