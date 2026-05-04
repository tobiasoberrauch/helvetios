package ch.swisstms.oms.ports;

import ch.swisstms.domain.ports.VenueGatewayPort;
import java.util.Optional;

/**
 * Looks up the {@link VenueGatewayPort} for a given MIC. Implementation is a Spring Bean that
 * injects every adapter and routes by configuration ({@code swisstms.venues.routing.MIC}).
 *
 * <p>Constitution Principle I — adding a new venue means adding a new implementation of {@code
 * VenueGatewayPort} as a Spring Bean. The OMS never references concrete adapters.
 */
public interface VenueRouter {
  Optional<VenueGatewayPort> resolve(String mic);
}
