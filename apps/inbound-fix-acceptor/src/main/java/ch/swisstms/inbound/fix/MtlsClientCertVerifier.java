package ch.swisstms.inbound.fix;

import java.security.cert.X509Certificate;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * mTLS client-certificate verifier (T242).
 *
 * <p>Every inbound FIX session presents an X.509 cert. We verify:
 *
 * <ul>
 *   <li>Subject CN matches the registered counterparty CN ({@code clients/*.yaml}).
 *   <li>Issuer O matches the swisstms internal CA.
 *   <li>NotAfter is at least 24h in the future (renew-before-expire safety net).
 * </ul>
 *
 * <p>Phase 14 layers in CRL / OCSP checks; for Phase 13 the in-process trust-store check that the
 * JDK does on the TLS handshake is sufficient — this verifier is the application-layer
 * second-factor that maps cert → counterparty identity.
 */
@Component
public class MtlsClientCertVerifier {

  private static final Logger log = LoggerFactory.getLogger(MtlsClientCertVerifier.class);
  private static final String EXPECTED_ISSUER_O = "Swiss-TMS Internal CA";

  public record VerificationResult(boolean ok, String reason, String resolvedClientCn) {}

  public VerificationResult verify(X509Certificate cert, String expectedCommonName) {
    if (cert == null) {
      return new VerificationResult(false, "no client certificate presented", null);
    }
    if (cert.getNotAfter().toInstant().isBefore(Instant.now().plusSeconds(86_400))) {
      return new VerificationResult(false, "client cert expires within 24h", null);
    }
    String subjectCn = extractCn(cert.getSubjectX500Principal().getName());
    String issuerO = extractO(cert.getIssuerX500Principal().getName());
    if (!EXPECTED_ISSUER_O.equals(issuerO)) {
      return new VerificationResult(false, "issuer O is not " + EXPECTED_ISSUER_O, subjectCn);
    }
    if (!expectedCommonName.equalsIgnoreCase(subjectCn)) {
      return new VerificationResult(
          false, "CN " + subjectCn + " does not match expected " + expectedCommonName, subjectCn);
    }
    log.debug("mTLS verified — subject CN={}", subjectCn);
    return new VerificationResult(true, "ok", subjectCn);
  }

  private static String extractCn(String dn) {
    return extract(dn, "CN");
  }

  private static String extractO(String dn) {
    return extract(dn, "O");
  }

  private static String extract(String dn, String key) {
    for (String part : dn.split(",")) {
      String trimmed = part.trim();
      if (trimmed.startsWith(key + "=")) {
        return trimmed.substring(key.length() + 1);
      }
    }
    return "";
  }
}
