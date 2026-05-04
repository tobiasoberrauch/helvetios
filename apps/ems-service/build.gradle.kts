plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Swiss-TMS — EMS / Aeron Cluster + Disruptor + algos (Phase 13)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.aeron:aeron-all:1.45.0")
    implementation("com.lmax:disruptor:4.0.0")
    implementation("net.openhft:chronicle-queue:5.27ea11") // compliance journal

    implementation(project(":libs:domain-model"))
    implementation(project(":libs:aeron-transport"))
    implementation(project(":libs:sbe-codec"))
    implementation(project(":libs:time-sync"))
    implementation(project(":libs:audit-chain"))
    implementation(project(":libs:observability"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
