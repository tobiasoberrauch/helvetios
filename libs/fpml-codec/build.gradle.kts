plugins { `java-library` }

description = "Swiss-TMS — FpML 5.12 codec via JAXB codegen."

ext["jaxbSchemas"] = files(rootProject.fileTree("contracts/fpml") { include("*.xsd") })
ext["jaxbPackage"] = "ch.swisstms.fpml.v5_12"

apply(from = "${rootProject.projectDir}/tools/codegen/jaxb.gradle.kts")

dependencies {
    api(project(":libs:domain-model"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("net.jqwik:jqwik:1.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
