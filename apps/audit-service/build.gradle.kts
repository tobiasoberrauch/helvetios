plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — Audit Chain Service: Kafka consumer, OpenSearch indexer, S3 WORM writer, daily verifier."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka:3.2.4")
    implementation("org.opensearch.client:opensearch-rest-high-level-client:2.16.0")
    implementation("software.amazon.awssdk:s3:2.27.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:audit-chain"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
