/*
 * Swiss Trading & Market Support Platform — root build.
 *
 * The root project itself is empty; subprojects pick up the convention
 * defined here via the `subprojects { ... }` block.
 */

plugins {
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("checkstyle")
    id("jacoco")
    id("idea")
}

allprojects {
    group = "ch.swisstms"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://packages.confluent.io/maven/") } // Schema Registry / kafka-avro
        // Vendor-specific repositories (Bloomberg BLPAPI etc.) live in
        // infra/maven-mirror/ and are added per-subproject where needed.
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")

    // Java 21 toolchain — Gradle will provision it via Foojay if the host
    // JDK is older. Our Constitution principle II (latency-hierarchy) and
    // principle IV (time-sync) rely on virtual threads and the modern
    // foreign function & memory API, both stabilised in JDK 21.
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            targetExclude("**/module-info.java", "**/build/generated/**")
            googleJavaFormat("1.22.0")
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint("1.2.1")
        }
    }

    extensions.configure<CheckstyleExtension> {
        toolVersion = "10.17.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
        maxWarnings = 0
    }

    tasks.withType<Checkstyle>().configureEach {
        exclude("**/module-info.java")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        finalizedBy("jacocoTestReport")
        testLogging {
            events("passed", "failed", "skipped")
            showExceptions = true
            showStackTraces = true
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:all,-serial,-processing",
                "-Werror",
                "-parameters",
            ),
        )
    }
}

// Custom convenience tasks that the top-level Makefile delegates to.
tasks.register("scaffold") {
    group = "build"
    description = "Generate SBE / JAXB / Avro codecs and download vendor mirrors."
    // Phase 2 will wire generators (T026, T027) into this task.
}
