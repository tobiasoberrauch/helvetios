plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — Reporting Service: FinfraG Art.39, RTS-22, Trax APA, EMIR (US5)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka:3.2.4")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")

    implementation("software.amazon.awssdk:s3:2.27.7") // WORM archival
    implementation("com.jcraft:jsch:0.1.55") // SFTP

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:fixml-codec"))
    implementation(project(":libs:fpml-codec"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:audit-chain"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
