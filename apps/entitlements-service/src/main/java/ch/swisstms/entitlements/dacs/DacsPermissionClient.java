package ch.swisstms.entitlements.dacs;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Refinitiv DACS / OpenDACS permission client (T165).
 *
 * <p>DACS gates market-data access via PROD_PERM FID 1 PE-codes; every Refinitiv RIC / instrument
 * carries one or more PE-codes, and a user is entitled iff their PE-code set is a superset.
 *
 * <p>Phase 8 ships an in-process map populated from {@code application.yml} so unit tests stay
 * deterministic. Phase 14 wires the real DACS daemon over TREP-RT (Refinitiv Real-Time Distribution
 * System) for live entitlement updates.
 */
@Component
public class DacsPermissionClient {

  private static final Logger log = LoggerFactory.getLogger(DacsPermissionClient.class);

  /** subjectId → set of PE-codes the subject is entitled to. */
  private final Map<String, Set<String>> userPeCodes = new ConcurrentHashMap<>();

  /** ric → set of PE-codes required to consume this RIC. */
  private final Map<String, Set<String>> ricPeCodes = new ConcurrentHashMap<>();

  public void putUser(String subjectId, Set<String> peCodes) {
    userPeCodes.put(subjectId, Set.copyOf(peCodes));
    log.debug("DACS user {} mapped to {} PE-codes", subjectId, peCodes.size());
  }

  public void putRic(String ric, Set<String> requiredPeCodes) {
    ricPeCodes.put(ric, Set.copyOf(requiredPeCodes));
  }

  /** Returns true iff the user holds every PE-code the RIC demands. */
  public boolean isPermitted(String subjectId, String ric) {
    Set<String> userCodes = userPeCodes.getOrDefault(subjectId, Set.of());
    Set<String> ricCodes = ricPeCodes.getOrDefault(ric, Set.of());
    if (ricCodes.isEmpty()) {
      // No PE-code on the RIC ⇒ public data, anyone can consume.
      return true;
    }
    return userCodes.containsAll(ricCodes);
  }
}
