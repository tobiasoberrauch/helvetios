plugins { `java-library` }

description = "Swiss-TMS — Kafka producer/consumer wrappers + Apicurio Avro + tier-prefix validation."

dependencies {
    api("org.apache.kafka:kafka-clients:3.8.0")
    api("io.apicurio:apicurio-registry-serdes-avro-serde:2.6.4.Final")

    api(project(":libs:domain-model"))
    api(project(":libs:observability"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.testcontainers:kafka:1.20.2")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
