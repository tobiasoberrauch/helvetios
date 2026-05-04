package ch.swisstms.venue.six;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone process for the SIX adapter when deployed independently. In Phase 1 the adapter is
 * also packaged as a JAR that the OMS-service loads as a dependency for in-process testing — see
 * SixVenueAdapterPort Spring beans below.
 */
@SpringBootApplication
public class SixVenueAdapterApplication {
  public static void main(String[] args) {
    SpringApplication.run(SixVenueAdapterApplication.class, args);
  }
}
