plugins { `java-library` }

description = "Swiss-TMS — Hash-chained audit log writer (Constitution VI)."

dependencies {
    api(project(":libs:domain-model"))
    api(project(":libs:time-sync"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
