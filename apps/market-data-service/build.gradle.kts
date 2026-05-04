plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — Market Data normalisation + Aeron multicast fan-out (US6)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka:3.2.4")
    implementation("io.aeron:aeron-all:1.45.0")
    implementation("org.questdb:questdb:8.3.2")
    implementation("com.clickhouse:clickhouse-jdbc:0.6.5")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:sbe-codec"))
    implementation(project(":libs:aeron-transport"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
