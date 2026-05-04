package ch.swisstms.venue.refinitiv;

import ch.swisstms.domain.instrument.InstrumentId;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * OpenDACS bridge — looks up the PE-codes that gate a given instrument and asks the central DACS
 * daemon whether {@code subjectId} holds them all (T173).
 *
 * <p>Phase 8 keeps both maps in-process so the adapter is unit-testable. Phase 14 swaps in (a) the
 * central {@code entitlements-service} as the source of {@code subjectId → PE-code} mappings and
 * (b) the Refinitiv reference-data feed for the per-instrument PE-code list.
 */
@Component
public class RefinitivOpenDacsBridge {

  private static final Logger log = LoggerFactory.getLogger(RefinitivOpenDacsBridge.class);

  /** subjectId → set of PE-codes the user holds. */
  private final Map<String, Set<String>> userPeCodes = new ConcurrentHashMap<>();

  /** instrument key (mic:isin) → required PE-codes. */
  private final Map<String, Set<String>> instrumentPeCodes = new ConcurrentHashMap<>();

  public void putUser(String subjectId, Set<String> peCodes) {
    userPeCodes.put(subjectId, Set.copyOf(peCodes));
  }

  public void putInstrument(InstrumentId instrument, Set<String> requiredPeCodes) {
    instrumentPeCodes.put(key(instrument), Set.copyOf(requiredPeCodes));
  }

  public boolean isPermitted(String subjectId, InstrumentId instrument) {
    Set<String> required = instrumentPeCodes.getOrDefault(key(instrument), Set.of());
    if (required.isEmpty()) {
      return true; // public instrument — no PE-code gate.
    }
    Set<String> held = userPeCodes.getOrDefault(subjectId, Set.of());
    boolean ok = held.containsAll(required);
    if (!ok) {
      log.debug(
          "DACS deny: subject={} instrument={} required={} held={}",
          subjectId,
          key(instrument),
          required,
          held);
    }
    return ok;
  }

  private static String key(InstrumentId i) {
    return i.mic() + ":" + i.isin();
  }
}
