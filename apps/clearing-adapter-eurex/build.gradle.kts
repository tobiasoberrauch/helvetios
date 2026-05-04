plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — Eurex Clearing FIXML over AMQP 1.0 (US4)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework:spring-jms")
    implementation("jakarta.jms:jakarta.jms-api:3.1.0")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.apache.qpid:qpid-jms-client:2.5.0") // AMQP 1.0
    implementation("org.springframework.kafka:spring-kafka:3.2.4")
    implementation("com.jcraft:jsch:0.1.55") // SFTP for CRE

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:fixml-codec"))
    implementation(project(":libs:fpml-codec"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:audit-chain"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.apache.qpid:qpid-broker:9.2.0") {
        // BDB JE is the persistent store for Qpid; tests use the in-memory store, so exclude it.
        exclude(group = "com.sleepycat")
    }
    testImplementation("net.jqwik:jqwik:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
