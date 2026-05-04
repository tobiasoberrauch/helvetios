package ch.swisstms.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T104 — Architectural fitness functions enforcing Constitution Principle I (Hexagonal Adapter
 * Discipline) and Principle IV (Time-Sync as First-Class) at build time.
 *
 * <p>This is the mechanical defence of the architecture. CI runs this suite on every PR; a
 * violation fails the build. CODEOWNERS adds the review-time defence; this test adds the
 * compile-time defence.
 */
class HexagonalArchitectureTest {

  private static JavaClasses platformClasses;

  @BeforeAll
  static void importClasses() {
    platformClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ch.swisstms");
  }

  @Test
  void domainCoreContainsNoVenueProtocolDetails() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("ch.swisstms.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "quickfix..", // FIX engine — adapters only
                "uk.co.real_logic..", // SBE / Aeron / Artio — adapters only
                "io.aeron..",
                "org.agrona..",
                "com.bloomberg..",
                "com.refinitiv..",
                "com.bidfx..",
                "org.apache.qpid..",
                "io.confluent..",
                "org.springframework..", // Spring belongs in apps/, not in domain/
                "jakarta.persistence..",
                "org.apache.kafka..",
                "io.micrometer..")
            .because(
                "Constitution Principle I — domain core MUST NOT reference "
                    + "venue, vendor, or transport-specific libraries");
    rule.check(platformClasses);
  }

  @Test
  void onlyDomainModelDeclaresPorts() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Port")
            .should()
            .resideInAPackage("ch.swisstms.domain.ports..")
            .because("All ports live in libs/domain-model/ports — adapters implement them");
    rule.check(platformClasses);
  }

  @Test
  void domainPortsAreInterfaces() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("ch.swisstms.domain.ports..")
            .and()
            .haveSimpleNameEndingWith("Port")
            .should()
            .beInterfaces()
            .because("Ports are abstractions; concrete classes belong in adapter modules");
    rule.check(platformClasses);
  }

  @Test
  void venueAdaptersDoNotDependOnEachOther() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("ch.swisstms.venue.six..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "ch.swisstms.venue.eurex..",
                "ch.swisstms.venue.bloomberg..",
                "ch.swisstms.venue.refinitiv..",
                "ch.swisstms.venue.tradeweb..",
                "ch.swisstms.venue.marketaxess..",
                "ch.swisstms.venue.bidfx..")
            .because(
                "Constitution Principle I — venue adapters are independent; "
                    + "cross-adapter coupling reintroduces venue lock-in");
    rule.check(platformClasses);
  }

  @Test
  void domainCodeDoesNotCallWallClock() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("ch.swisstms.domain..")
            .should()
            .callMethod(System.class, "currentTimeMillis")
            .orShould()
            .callMethod(System.class, "nanoTime")
            .because("Constitution Principle IV — use libs/time-sync RegulatoryClock");
    rule.check(platformClasses);
  }
}
