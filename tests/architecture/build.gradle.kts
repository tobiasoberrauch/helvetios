plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.6"
}

description = "Architectural fitness functions enforcing the constitution at build time."

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
    }
}

dependencies {
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")

    // Pull in every JVM module of the platform so ArchUnit can scan them.
    testImplementation(project(":libs:domain-model"))
    testImplementation(project(":libs:fix-codec"))
    testImplementation(project(":libs:sbe-codec"))
    testImplementation(project(":libs:fixml-codec"))
    testImplementation(project(":libs:fpml-codec"))
    testImplementation(project(":libs:audit-chain"))
    testImplementation(project(":libs:time-sync"))
    testImplementation(project(":libs:observability"))
    testImplementation(project(":libs:security"))
    testImplementation(project(":libs:kafka-transport"))
    testImplementation(project(":libs:aeron-transport"))
    testImplementation(project(":libs:pretrade-risk"))

    testImplementation(project(":apps:oms-service"))
    testImplementation(project(":apps:venue-adapter-six"))
    testImplementation(project(":apps:reconciler-service"))

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
