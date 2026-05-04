package ch.swisstms.domain.health;

/** Constitution Principle II — three latency tiers. */
public enum LatencyTier {
  /** Sub-100µs deterministic. Aeron IPC + SBE + Disruptor. */
  HOT,
  /** Sub-5ms. Aeron UDP / Solace / IBM MQ Low-Latency. */
  WARM,
  /** Seconds. Kafka + Postgres + S3. */
  COLD
}
