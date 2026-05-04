package ch.swisstms.oms.infra;

import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.domain.common.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditChainConfig {

  @Bean
  public HashChainWriter auditWriter(@Value("${swisstms.region:ZH}") String region) {
    // Phase 3 — fresh-start chain. Phase 15 (audit-service) bootstraps
    // from the last-known hash in S3 WORM.
    return new HashChainWriter(Region.valueOf(region), 0L, new byte[32]);
  }
}
