plugins { `java-library` }

description = "Swiss-TMS — Generated SBE codecs + Aeron channel naming conventions."

apply(from = "${rootProject.projectDir}/tools/codegen/sbe-codec-generator.gradle.kts")

dependencies {
    api(project(":libs:domain-model"))
    api(project(":libs:time-sync"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
