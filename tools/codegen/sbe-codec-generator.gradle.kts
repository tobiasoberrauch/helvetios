/*
 * SBE codegen Gradle task. Wired into `libs/sbe-codec/build.gradle.kts`
 * (Phase 2). The task generates Java codecs from the schemas in
 * `contracts/sbe/*.xml`. Generated sources are committed to
 * `libs/sbe-codec/src/generated/` and ignored by Spotless.
 *
 * Usage from any subproject:
 *
 *   apply(from = "$rootDir/tools/codegen/sbe-codec-generator.gradle.kts")
 *
 * The applying project will gain a `:sbeGenerate` task and have its
 * generated sources included in the `main` SourceSet.
 */

plugins {
    `java-library`
}

val sbeVersion = "1.31.1"

configurations.create("sbeTool")

dependencies {
    "sbeTool"("uk.co.real-logic:sbe-tool:$sbeVersion")
    api("uk.co.real-logic:sbe-tool:$sbeVersion")
    api("org.agrona:agrona:1.21.2")
}

val generatedSrcDir = layout.buildDirectory.dir("generated/sbe")

val sbeGenerate by tasks.registering(JavaExec::class) {
    group = "codegen"
    description = "Generate Java SBE codecs from contracts/sbe/*.xml"
    classpath = configurations["sbeTool"]
    mainClass.set("uk.co.real_logic.sbe.SbeTool")

    val schemaDir = rootProject.file("contracts/sbe")
    val schemas = schemaDir.listFiles { _, name -> name.endsWith(".xml") }?.sortedBy { it.name } ?: emptyList()

    inputs.files(schemas)
    outputs.dir(generatedSrcDir)

    args = listOf(
        "-Dsbe.target.language=Java",
        "-Dsbe.output.dir=${generatedSrcDir.get().asFile.absolutePath}",
        "-Dsbe.generate.ir=true",
        "-Dsbe.java.generate.interfaces=true",
    ) + schemas.map { it.absolutePath }

    doFirst {
        generatedSrcDir.get().asFile.mkdirs()
    }
}

extensions.configure<SourceSetContainer>("sourceSets") {
    named("main") {
        java.srcDir(generatedSrcDir)
    }
}

tasks.named("compileJava") {
    dependsOn(sbeGenerate)
}
