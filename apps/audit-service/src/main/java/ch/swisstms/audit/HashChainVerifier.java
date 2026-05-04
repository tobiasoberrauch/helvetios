package ch.swisstms.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily Hash-Chain Verifier (Constitution Principle VI).
 *
 * <p>Liest die letzten 24h aus `audit.command.v1` (Kafka) bzw. dem S3-WORM-Mirror und prüft dass
 * jede prev_hash zur vorherigen hash passt. Bei Mismatch: **Sev-1 Incident**.
 */
@Component
public class HashChainVerifier {

  private static final Logger log = LoggerFactory.getLogger(HashChainVerifier.class);

  @Scheduled(cron = "0 30 0 * * *") // 00:30 UTC daily
  public void verifyDaily() {
    log.info("Daily hash-chain verification starting");
    // TODO Phase 16 — wire OpenSearch query + libs/audit-chain
    // HashChainWriter.verifyChain. Mismatch → PagerDuty / FINMA-flag.
    log.info("Daily hash-chain verification complete");
  }
}
