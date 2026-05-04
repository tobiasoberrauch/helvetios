package ch.swisstms.region;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * YAML routing-rule reader (T262).
 *
 * <p>Three lookup tables drive cross-region routing:
 *
 * <ul>
 *   <li>{@code clientPreferredRegion} — per-client preferred region (e.g. EU client → ZH primary
 *       with LD4 fallback).
 *   <li>{@code instrumentPrimaryRegion} — per-instrument primary venue region (e.g. SIX NESN.S →
 *       ZH; CME futures → NY4).
 *   <li>{@code assetClassMarketHours} — per-asset-class market-hours window so we stop routing to a
 *       closed venue before the cutover scheduler kicks in.
 * </ul>
 *
 * <p>Format lives in {@code application.yml} under {@code swisstms.routing.*}; production
 * deployments override per-region via Helm.
 */
@Component
public class RoutingRulesLoader {

  private static final Logger log = LoggerFactory.getLogger(RoutingRulesLoader.class);

  public record MarketHours(String openUtc, String closeUtc) {}

  private final Map<String, String> clientPreferredRegion = new HashMap<>();
  private final Map<String, String> instrumentPrimaryRegion = new HashMap<>();
  private final Map<String, MarketHours> assetClassMarketHours = new HashMap<>();

  public RoutingRulesLoader(
      @Value("${swisstms.routing.rules-resource:classpath:routing-rules.yaml}")
          Resource rulesResource) {
    if (rulesResource != null && rulesResource.exists()) {
      load(rulesResource);
    } else {
      // Use sensible defaults so the router boots in dev without a config file.
      seedDefaults();
    }
  }

  @SuppressWarnings("unchecked")
  private void load(Resource resource) {
    try (var in = resource.getInputStream()) {
      var opts = new LoaderOptions();
      Yaml yaml = new Yaml(new SafeConstructor(opts));
      Map<String, Object> root = yaml.load(in);
      if (root == null) {
        seedDefaults();
        return;
      }
      Map<String, Object> swisstms = (Map<String, Object>) root.getOrDefault("swisstms", Map.of());
      Map<String, Object> routing =
          (Map<String, Object>) swisstms.getOrDefault("routing", Map.of());
      Map<String, String> clients =
          (Map<String, String>) routing.getOrDefault("clientPreferredRegion", Map.of());
      clientPreferredRegion.putAll(clients);
      Map<String, String> instruments =
          (Map<String, String>) routing.getOrDefault("instrumentPrimaryRegion", Map.of());
      instrumentPrimaryRegion.putAll(instruments);
      Map<String, Map<String, String>> hours =
          (Map<String, Map<String, String>>)
              routing.getOrDefault("assetClassMarketHours", Map.of());
      hours.forEach(
          (ac, kv) ->
              assetClassMarketHours.put(
                  ac,
                  new MarketHours(
                      kv.getOrDefault("openUtc", "00:00"), kv.getOrDefault("closeUtc", "24:00"))));
      log.info(
          "Loaded routing rules: {} clients, {} instruments, {} asset-classes",
          clientPreferredRegion.size(),
          instrumentPrimaryRegion.size(),
          assetClassMarketHours.size());
    } catch (Exception e) {
      log.warn("Failed to load routing rules ({}), falling back to defaults", e.getMessage());
      seedDefaults();
    }
  }

  private void seedDefaults() {
    instrumentPrimaryRegion.put("XSWX", "ZH");
    instrumentPrimaryRegion.put("XEUR", "ZH");
    instrumentPrimaryRegion.put("XLON", "LD4");
    instrumentPrimaryRegion.put("XNAS", "NY4");
    instrumentPrimaryRegion.put("XNYS", "NY4");
    instrumentPrimaryRegion.put("XTKS", "TY3");
    assetClassMarketHours.put("EQUITY_CH", new MarketHours("07:00", "15:30"));
    assetClassMarketHours.put("EQUITY_LSE", new MarketHours("08:00", "16:30"));
    assetClassMarketHours.put("EQUITY_US", new MarketHours("13:30", "20:00"));
    assetClassMarketHours.put("EQUITY_JP", new MarketHours("00:00", "06:00"));
  }

  public String regionForClient(String clientId, String fallback) {
    return clientPreferredRegion.getOrDefault(clientId, fallback);
  }

  public String regionForInstrument(String mic, String fallback) {
    return instrumentPrimaryRegion.getOrDefault(mic, fallback);
  }

  public MarketHours hoursForAssetClass(String assetClass) {
    return assetClassMarketHours.get(assetClass);
  }

  public int clientCount() {
    return clientPreferredRegion.size();
  }

  public int instrumentCount() {
    return instrumentPrimaryRegion.size();
  }
}
