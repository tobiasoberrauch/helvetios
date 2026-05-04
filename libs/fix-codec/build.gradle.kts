plugins { `java-library` }

description = "Swiss-TMS — QuickFIX/J wrappers + Postgres-backed JdbcStoreFactory."

dependencies {
    api("org.quickfixj:quickfixj-core:2.3.2")
    api("org.quickfixj:quickfixj-messages-fix44:2.3.2")
    api("org.quickfixj:quickfixj-messages-fix50sp2:2.3.2")
    api("org.quickfixj:quickfixj-messages-fixt11:2.3.2")
    api("org.postgresql:postgresql:42.7.4")
    api("io.micrometer:micrometer-core:1.13.4")

    api(project(":libs:domain-model"))
    api(project(":libs:time-sync"))
    api(project(":libs:audit-chain"))
    api(project(":libs:observability"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testImplementation("org.testcontainers:postgresql:1.20.2")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
