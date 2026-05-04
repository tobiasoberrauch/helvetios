package ch.swisstms.inbound.fix;

import java.util.List;

/**
 * Per-client session configuration loaded from {@code clients/*.yaml} (T241).
 *
 * <p>Mirrors the YAML shape verbatim so Snake YAML / Jackson can deserialise without custom binding
 * code. Mutable POJOs (not records) because Spring Boot's YamlPropertiesFactoryBean binds via
 * setters.
 */
public class ClientSessionConfig {

  public Client client;
  public Session session;
  public Throttle throttle;
  public DropCopy dropCopy;
  public Risk risk;
  public Permissions permissions;

  public static class Client {
    public String id;
    public String legalEntityId;
    public String status;
    public String preferredRegion;
    public String fallbackRegion;
  }

  public static class Session {
    public String senderCompId;
    public String targetCompId;
    public String fixVersion;
    public List<String> inboundIpAllowList;
    public String encryption;
    public int heartbeatIntervalSec;
  }

  public static class Throttle {
    public int ordersPerSecond;
    public int inFlightOrders;
    public String rejectStrategy;
  }

  public static class DropCopy {
    public boolean enabled;
    public boolean separateSession;
    public String senderCompId;
    public String targetCompId;
  }

  public static class Risk {
    public String riskProfileId;
    public java.math.BigDecimal fatFingerNotional;
    public java.math.BigDecimal fatFingerQuantity;
    public java.math.BigDecimal maxOrderSizeNotional;
    public java.util.Map<String, java.math.BigDecimal> notionalDailyLimits;
  }

  public static class Permissions {
    public List<String> permittedAssetClasses;
    public Routing routing;
  }

  public static class Routing {
    public List<String> allowedModes;
    public List<String> allowedAlgos;
    public String defaultMode;
  }
}
