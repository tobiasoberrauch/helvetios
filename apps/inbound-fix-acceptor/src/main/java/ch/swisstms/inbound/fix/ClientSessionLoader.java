package ch.swisstms.inbound.fix;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Loads per-client session configs from {@code classpath:clients/*.yaml} (T241).
 *
 * <p>Phase 13 keeps the configs on the classpath so they ship with the container image and the
 * acceptor can boot without a control-plane round-trip. Phase 14 will swap in a Spring Cloud Config
 * server backed by OpenBao + Postgres so client onboarding does not require a redeploy.
 */
@Component
public class ClientSessionLoader {

  private static final Logger log = LoggerFactory.getLogger(ClientSessionLoader.class);

  private final Map<String, ClientSessionConfig> byClientId = new HashMap<>();

  public ClientSessionLoader() {
    loadAllInternal("classpath:clients/*.yaml");
  }

  public final void loadAll(String pattern) {
    loadAllInternal(pattern);
  }

  private void loadAllInternal(String pattern) {
    try {
      var loaderOpts = new LoaderOptions();
      loaderOpts.setAllowDuplicateKeys(false);
      Yaml yaml = new Yaml(new Constructor(ClientSessionConfig.class, loaderOpts));
      var resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources(pattern);
      for (Resource r : resources) {
        try (var in = r.getInputStream()) {
          ClientSessionConfig cfg = yaml.load(in);
          if (cfg == null || cfg.client == null) {
            log.warn("Skipping malformed client config {}", r.getFilename());
            continue;
          }
          byClientId.put(cfg.client.id, cfg);
          log.info("Loaded client session config {}", cfg.client.id);
        }
      }
    } catch (IOException e) {
      log.error("Failed to load client session configs: {}", e.getMessage());
    }
  }

  public ClientSessionConfig get(String clientId) {
    return byClientId.get(clientId);
  }

  public Map<String, ClientSessionConfig> all() {
    return Map.copyOf(byClientId);
  }
}
