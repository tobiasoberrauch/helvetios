plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — SIX Swiss Exchange venue adapter (STI / OTI / QTI / IMI / TRI)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.kafka:spring-kafka:3.2.4") // drop-copy producer
    implementation("com.jcraft:jsch:0.1.55") // T299 TRI SFTP submitter

    // QuickFIX/J for the FIX 4.4 STI session.
    implementation("org.quickfixj:quickfixj-core:2.3.2")
    implementation("org.quickfixj:quickfixj-messages-fix44:2.3.2")
    // Spring-Boot-Starter optional — we wire QuickFIX manually for clarity.

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:fix-codec"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:audit-chain"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:1.20.2")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
