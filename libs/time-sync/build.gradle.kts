plugins { `java-library` }

description = "Swiss-TMS — RegulatoryClock (RTS-25 PHC-aware) + MonotonicClock."

dependencies {
    api(project(":libs:domain-model"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
