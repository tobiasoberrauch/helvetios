plugins { `java-library` }

description = "Swiss-TMS — FIXML 5.0 SP2 codec (Eurex C7) via JAXB codegen."

ext["jaxbSchemas"] = files(rootProject.fileTree("contracts/fixml") { include("*.xsd") })
ext["jaxbPackage"] = "ch.swisstms.fixml.eurex"

apply(from = "${rootProject.projectDir}/tools/codegen/jaxb.gradle.kts")

dependencies {
    api(project(":libs:domain-model"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
