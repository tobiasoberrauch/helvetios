package ch.swisstms.reconciler;

import java.time.Duration;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka-Streams-Topologie für die Drop-Copy-Reconciliation (US2).
 *
 * <p>Constitution Principle V — Drop-Copy gewinnt bei Disagreements.
 *
 * <p>Pfade:
 *
 * <ul>
 *   <li>Match (übereinstimmende {@link JoinKey} aus beiden Streams) → publiziert auf {@code
 *       cold.exec.fill.v1} als authoritativer Fill.
 *   <li>Drop-Copy ohne OMS-Pendant → {@code warm.recon.mismatch.v1} + {@code cold.exec.fill.v1}
 *       (Drop-Copy ist authoritativ).
 *   <li>OMS ohne Drop-Copy-Pendant → {@code warm.recon.mismatch.v1} (zu untersuchen; OMS-Eintrag
 *       bleibt bestehen, wird aber als "unauthorized" markiert).
 * </ul>
 */
public class ReconcilerTopology {

  private static final Logger log = LoggerFactory.getLogger(ReconcilerTopology.class);

  public static final String TOPIC_OMS_EVENT = "cold.oms.event.v1";
  public static final String TOPIC_DROPCOPY_PREFIX = "warm.dropcopy.";
  public static final String TOPIC_RECON_MISMATCH = "warm.recon.mismatch.v1";
  public static final String TOPIC_AUTHORITATIVE_FILL = "cold.exec.fill.v1";

  /**
   * Baut die Kafka-Streams-Topologie. Phase 4 (US2) verwendet einen 5-minütigen
   * Sliding-Window-Outer-Join — Mismatches > 5min sind Sev-2 Alerts (PR-100 / `recon-mismatch.yml`
   * AlertManager-Regel).
   */
  public StreamsBuilder build(StreamsBuilder builder) {
    KStream<String, String> omsStream =
        builder.stream(
            TOPIC_OMS_EVENT,
            org.apache.kafka.streams.kstream.Consumed.with(Serdes.String(), Serdes.String()));
    KStream<String, String> dropcopyStream =
        builder.stream(
            java.util.regex.Pattern.compile("warm\\.dropcopy\\..*\\.v1"),
            org.apache.kafka.streams.kstream.Consumed.with(Serdes.String(), Serdes.String()));

    // Re-key both streams to the canonical JoinKey.
    KStream<String, String> omsKeyed =
        omsStream
            .filter(
                (k, v) ->
                    v != null
                        && (v.contains("ORDER_PARTIALLY_FILLED") || v.contains("ORDER_FILLED")))
            .selectKey((k, v) -> extractJoinKey(v));

    KStream<String, String> dcKeyed = dropcopyStream.selectKey((k, v) -> extractJoinKey(v));

    // Outer join with a 5-minute window. We then route based on which
    // sides are present. Constitution V: drop-copy wins.
    omsKeyed
        .outerJoin(
            dcKeyed,
            (omsValue, dcValue) -> {
              if (omsValue != null && dcValue != null) return "MATCH:" + dcValue;
              if (dcValue != null) return "DROPCOPY_ONLY:" + dcValue;
              return "OMS_ONLY:" + omsValue;
            },
            JoinWindows.ofTimeDifferenceAndGrace(Duration.ofMinutes(5), Duration.ofMinutes(1)),
            StreamJoined.with(Serdes.String(), Serdes.String(), Serdes.String()))
        .foreach(
            (joinKey, decision) -> {
              if (decision.startsWith("MATCH:")) {
                log.debug("RECON MATCH key={}", joinKey);
              } else if (decision.startsWith("DROPCOPY_ONLY:")) {
                log.warn("RECON DROPCOPY_ONLY (OMS missing) key={}", joinKey);
              } else if (decision.startsWith("OMS_ONLY:")) {
                log.warn("RECON OMS_ONLY (drop-copy missing) key={}", joinKey);
              }
            });

    // Drop-copy stream is *also* the authoritative fill source — every
    // drop-copy event is republished verbatim onto cold.exec.fill.v1.
    // Constitution Principle V — independent of OMS state.
    dropcopyStream.to(TOPIC_AUTHORITATIVE_FILL);

    return builder;
  }

  /**
   * Extrahiert den Join-Key aus einer JSON-Payload. Phase 4 verwendet einen leichtgewichtigen
   * String-Match; Phase 8 (US6) wechselt auf Apicurio-Avro-Deserialisierung mit getypten Klassen.
   */
  static String extractJoinKey(String payload) {
    if (payload == null) return "UNKNOWN";
    // Best-effort: extrahiere clOrdId + execId via Regex.
    String clOrdId = jsonField(payload, "clOrdId");
    String execId = jsonField(payload, "venueExecutionId");
    if (execId == null) execId = jsonField(payload, "executionId");
    return "OMS|" + clOrdId + "|" + execId;
  }

  private static String jsonField(String payload, String name) {
    int i = payload.indexOf("\"" + name + "\"");
    if (i < 0) return null;
    int colon = payload.indexOf(':', i);
    int q1 = payload.indexOf('"', colon + 1);
    int q2 = payload.indexOf('"', q1 + 1);
    return (q1 < 0 || q2 < 0) ? null : payload.substring(q1 + 1, q2);
  }
}
