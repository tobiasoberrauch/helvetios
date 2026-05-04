plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — Bloomberg BLPAPI / EMSX / B-PIPE / DL adapter (US6 / Phase 8)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka:3.2.4")
    implementation("com.jcraft:jsch:0.1.55") // Bloomberg DL SFTP nightly pull

    // BLPAPI v3 — proprietary; pulled from infra/maven-mirror/ once the
    // Bloomberg artifact has been mavened locally. For now it's a TODO
    // dependency that is excluded from the default build to keep CI green
    // until the vendor mirror is wired (Phase 14).
    // implementation("com.bloomberg:blpapi:3.24.6.1")

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
