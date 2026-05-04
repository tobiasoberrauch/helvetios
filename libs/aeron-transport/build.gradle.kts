plugins { `java-library` }

description = "Swiss-TMS — Aeron Cluster bootstrap + Archive client."

dependencies {
    api("io.aeron:aeron-all:1.45.0")
    api("org.agrona:agrona:1.21.2")

    api(project(":libs:domain-model"))
    api(project(":libs:sbe-codec"))
    api(project(":libs:time-sync"))
    api(project(":libs:observability"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}
