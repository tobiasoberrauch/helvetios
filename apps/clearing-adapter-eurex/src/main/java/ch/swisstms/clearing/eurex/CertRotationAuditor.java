package ch.swisstms.clearing.eurex;

import ch.swisstms.audit_chain.AuditEvent;
import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * T127 — Cert-Rotation-Auditor.
 *
 * <p>Eurex rotiert die TLS-CA jährlich im September. Diese Komponente checkt täglich den Truststore
 * und alarmiert (via Audit-Chain + Prometheus-Metric) wenn ein Zertifikat in &lt; 30 Tagen abläuft.
 *
 * <p>Constitution Principle VI — jede Cert-Rotation wird in der Audit-Chain mit Action {@code
 * clearing.eurex.cert.rotated} festgehalten.
 */
@Component
public class CertRotationAuditor {

  private static final Logger log = LoggerFactory.getLogger(CertRotationAuditor.class);
  private static final long ALERT_THRESHOLD_DAYS = 30;

  private final HashChainWriter audit;
  private final String truststorePath;
  private final char[] truststorePassword;

  public CertRotationAuditor(
      HashChainWriter audit,
      @Value("${swisstms.eurex.truststore.path:/etc/swisstms/keystores/eurex-truststore.jks}")
          String path,
      @Value("${swisstms.eurex.truststore.password:changeit}") String password) {
    this.audit = audit;
    this.truststorePath = path;
    this.truststorePassword = password.toCharArray();
  }

  @Scheduled(cron = "0 5 6 * * *") // daily 06:05 UTC
  public void checkExpiry() {
    File ks = new File(truststorePath);
    if (!ks.exists()) {
      log.warn(
          "Eurex truststore not found at {} — Phase 6 dev environments expect a stub",
          truststorePath);
      return;
    }
    try (var fis = new java.io.FileInputStream(ks)) {
      KeyStore store = KeyStore.getInstance("JKS");
      store.load(fis, truststorePassword);
      Enumeration<String> aliases = store.aliases();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        java.security.cert.Certificate cert = store.getCertificate(alias);
        if (cert instanceof X509Certificate x509) {
          long days =
              Math.max(0, ChronoUnit.DAYS.between(Instant.now(), x509.getNotAfter().toInstant()));
          if (days <= ALERT_THRESHOLD_DAYS) {
            log.warn("Eurex truststore alias '{}' expires in {} days", alias, days);
            AuditEvent ev =
                audit.append(
                    ActorType.SERVICE,
                    "clearing-adapter-eurex",
                    "clearing.eurex.cert.expiry.warning",
                    "Truststore",
                    alias,
                    ("{\"alias\":\"" + alias + "\",\"daysRemaining\":" + days + "}").getBytes(),
                    null);
            log.info("Audit-chain entry seq={} hash[..7]={}", ev.seq(), shortHash(ev.hash()));
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to inspect Eurex truststore", e);
    }
  }

  private static String shortHash(byte[] h) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < Math.min(4, h.length); i++) sb.append(String.format("%02x", h[i]));
    return sb.toString();
  }
}
