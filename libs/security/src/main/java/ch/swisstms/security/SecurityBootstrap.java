package ch.swisstms.security;

/**
 * Zentrale Security-Konfiguration. Echte Implementierung wird in Phase 14 (Multi-Region)
 * fertiggestellt — bis dahin Stub.
 */
public final class SecurityBootstrap {
  private SecurityBootstrap() {}

  public static String openBaoEndpoint() {
    return System.getenv().getOrDefault("OPENBAO_ADDR", "http://localhost:8200");
  }

  public static String keycloakRealmUrl() {
    return System.getenv()
        .getOrDefault("KEYCLOAK_ISSUER_URI", "http://localhost:8180/realms/swiss-tms");
  }

  public static String spiffeTrustDomain() {
    return System.getenv().getOrDefault("SPIFFE_TRUST_DOMAIN", "swiss-tms.local");
  }
}
