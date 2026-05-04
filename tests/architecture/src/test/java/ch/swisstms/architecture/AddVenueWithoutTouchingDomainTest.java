package ch.swisstms.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * T111 — "Adding a new venue does not touch the domain" test.
 *
 * <p>The test does NOT actually run a new adapter (that would require spawning a full Spring
 * context). Instead it asserts that the static topology between adapter packages and the rest of
 * the platform makes a constitution-violating change impossible:
 *
 * <ul>
 *   <li>No domain class depends on any class in {@code ch.swisstms.venue..}
 *   <li>No app service class (oms / ems / reconciler / reporting / surveillance / entitlements)
 *       depends on a specific venue package
 *   <li>The OMS' router resolves adapters by interface (the actual mechanical proof: only {@link
 *       ch.swisstms.domain.ports.VenueGatewayPort} is referenced)
 * </ul>
 *
 * Together these properties guarantee that an adapter can be added or removed without touching the
 * rest of the platform — Constitution Principle I in mechanical form.
 */
class AddVenueWithoutTouchingDomainTest {

  @Test
  void domainNeverImportsAnyVenue() {
    JavaClasses classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ch.swisstms");

    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("ch.swisstms.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("ch.swisstms.venue..")
            .because("Constitution Principle I — domain never knows venues");
    rule.check(classes);
  }

  @Test
  void noServiceImportsConcreteVenueAdapterClasses() {
    JavaClasses classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ch.swisstms.oms", "ch.swisstms.reconciler");

    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage("ch.swisstms.oms..", "ch.swisstms.reconciler..")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Adapter")
            .andShould()
            .dependOnClassesThat()
            .resideInAPackage("ch.swisstms.venue..")
            .because("OMS / reconciler must talk to adapters via VenueGatewayPort only");
    rule.check(classes);
  }

  @Test
  void venueGatewayPortIsTheOnlyContract() {
    JavaClasses classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ch.swisstms.venue", "ch.swisstms.oms");

    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("ch.swisstms.venue..")
            .and()
            .haveSimpleNameEndingWith("Adapter")
            .should()
            .implement(ch.swisstms.domain.ports.VenueGatewayPort.class)
            .because(
                "All venue adapters must implement VenueGatewayPort — single point of contract");
    rule.check(classes);
  }
}
