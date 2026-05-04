package ch.swisstms.oms.infra;

import ch.swisstms.domain.ports.VenueGatewayPort;
import ch.swisstms.oms.ports.VenueRouter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SpringVenueRouter implements VenueRouter {

  private final Map<String, VenueGatewayPort> byMic = new HashMap<>();

  public SpringVenueRouter(List<VenueGatewayPort> adapters) {
    for (VenueGatewayPort a : adapters) {
      byMic.put(a.venueMic(), a);
    }
  }

  @Override
  public Optional<VenueGatewayPort> resolve(String mic) {
    return Optional.ofNullable(byMic.get(mic));
  }
}
