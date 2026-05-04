package ch.swisstms.kafka_transport;

import ch.swisstms.domain.health.LatencyTier;

/**
 * Constitution Principle II — Latency-Hierarchy-Validierung. Verhindert, dass eine Komponente ein
 * Topic der falschen Tier-Klasse konsumiert/produziert (z.B. ein hot-path Service auf {@code
 * cold.*}).
 */
public final class TierPrefixValidator {
  private TierPrefixValidator() {}

  public static void validateProducer(LatencyTier serviceTier, String topic) {
    String prefix = topic.split("\\.", 2)[0];
    switch (prefix) {
      case "hot" -> require(serviceTier == LatencyTier.HOT, serviceTier, topic);
      case "warm" -> require(serviceTier != LatencyTier.HOT, serviceTier, topic);
      case "cold" -> require(true, serviceTier, topic);
      case "audit" -> require(true, serviceTier, topic);
      case "tca", "region" -> require(true, serviceTier, topic);
      default ->
          throw new IllegalArgumentException("Topic '" + topic + "' has no recognized tier prefix");
    }
  }

  private static void require(boolean ok, LatencyTier serviceTier, String topic) {
    if (!ok) {
      throw new IllegalArgumentException(
          "Latency-tier mismatch: service=" + serviceTier + " topic=" + topic);
    }
  }
}
