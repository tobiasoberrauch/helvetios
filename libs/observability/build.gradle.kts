plugins { `java-library` }

description = "Swiss-TMS — OpenTelemetry SDK + Micrometer + Aeron-counters Prometheus exporter."

dependencies {
    api("io.opentelemetry:opentelemetry-api:1.42.1")
    api("io.opentelemetry:opentelemetry-sdk:1.42.1")
    api("io.opentelemetry:opentelemetry-exporter-otlp:1.42.1")
    api("io.micrometer:micrometer-core:1.13.4")
    api("io.micrometer:micrometer-registry-prometheus:1.13.4")
    api("ch.qos.logback:logback-classic:1.5.8")
    api("net.logstash.logback:logstash-logback-encoder:8.0")

    api(project(":libs:domain-model"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
