/*
 * Constitution Principle I — Hexagonal Adapter Discipline.
 *
 * The domain core exports only ports and value objects. Adapter modules
 * (apps/venue-adapter-*, apps/clearing-adapter-*) MUST depend on these
 * exports and on nothing else internal to the domain.
 *
 * NOTE: This module-info is currently advisory — Spring Boot's classpath
 * model and Spotless's processing don't fully honour the JPMS module
 * graph at build time. The same constraint is enforced by:
 *   - .github/CODEOWNERS (review-time)
 *   - tests/architecture/HexagonalArchitectureTest.java (build-time, ArchUnit)
 *
 * This file documents the intent at compile-time for forward-compatible
 * tooling.
 */
module ch.swisstms.domain {
    requires java.base;
    requires transitive java.sql;

    exports ch.swisstms.domain.client;
    exports ch.swisstms.domain.common;
    exports ch.swisstms.domain.execution;
    exports ch.swisstms.domain.health;
    exports ch.swisstms.domain.instrument;
    exports ch.swisstms.domain.marketdata;
    exports ch.swisstms.domain.order;
    exports ch.swisstms.domain.ports;
    exports ch.swisstms.domain.price;
}
