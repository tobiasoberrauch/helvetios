plugins { `java-library` }

description = "Swiss-TMS — OAuth2/OIDC, mTLS, OpenBao, SPIFFE attestation."

dependencies {
    // OpenBao Java driver and io.spiffe artefacts not yet on Maven Central as of 2026-05.
    // Phase 14 will wire the real coordinates; until then the public surface is
    // declared via interface stubs in this module.
    api("io.spiffe:java-spiffe-core:0.8.17")

    api(project(":libs:domain-model"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

// Bibliotheks-Coordinaten oben sind teilweise noch nicht öffentlich
// gepublished — daher per opt-in aktivierbar. Phase 14 stellt das fertig.
configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}
