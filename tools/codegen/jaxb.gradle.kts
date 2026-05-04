/*
 * JAXB codegen Gradle task. Wired into `libs/fixml-codec/build.gradle.kts`,
 * `libs/fpml-codec/build.gradle.kts`, and any service that needs to
 * generate Java classes from XSDs (e.g., ISO 20022 SECOM messages).
 *
 * Usage:
 *
 *   apply(from = "$rootDir/tools/codegen/jaxb.gradle.kts")
 *   ext["jaxbSchemas"] = files("$rootDir/contracts/fixml/*.xsd")
 *   ext["jaxbPackage"] = "ch.swisstms.fixml.eurex.c7"
 *
 * The :jaxbGenerate task produces Java sources under
 * build/generated/jaxb/ which are added to the main SourceSet.
 */

plugins {
    `java-library`
}

val jaxbVersion = "4.0.5"

configurations.create("jaxbTool")

dependencies {
    "jaxbTool"("org.glassfish.jaxb:jaxb-xjc:$jaxbVersion")
    "jaxbTool"("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
    api("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    api("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
}

val generatedJaxbSrcDir = layout.buildDirectory.dir("generated/jaxb")

val jaxbGenerate by tasks.registering(JavaExec::class) {
    group = "codegen"
    description = "Generate Java classes from XSDs configured by ext.jaxbSchemas + ext.jaxbPackage"

    classpath = configurations["jaxbTool"]
    mainClass.set("com.sun.tools.xjc.XJCFacade")

    val schemas = (project.findProperty("jaxbSchemas") as? FileCollection)
        ?: rootProject.files()
    val pkg = (project.findProperty("jaxbPackage") as? String) ?: "ch.swisstms.generated"

    inputs.files(schemas)
    inputs.property("package", pkg)
    outputs.dir(generatedJaxbSrcDir)

    doFirst {
        generatedJaxbSrcDir.get().asFile.mkdirs()
    }

    args = listOf(
        "-d", generatedJaxbSrcDir.get().asFile.absolutePath,
        "-p", pkg,
        "-quiet",
    ) + schemas.files.map { it.absolutePath }
}

extensions.configure<SourceSetContainer>("sourceSets") {
    named("main") {
        java.srcDir(generatedJaxbSrcDir)
    }
}

tasks.named("compileJava") {
    dependsOn(jaxbGenerate)
}
