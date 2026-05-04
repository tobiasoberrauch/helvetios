plugins {
    `java-library`
}

description = "Swiss-TMS — Domain model. Value objects, aggregates, ports."

dependencies {
    // Domain code MUST NOT depend on any venue-, vendor-, or transport-specific
    // library. Constitution Principle I (Hexagonal Adapter Discipline).
    api("org.jspecify:jspecify:1.0.0")
    api("io.projectreactor:reactor-core:3.6.10") // Flow.Publisher equivalents

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
