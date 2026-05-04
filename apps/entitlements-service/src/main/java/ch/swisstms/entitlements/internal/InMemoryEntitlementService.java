package ch.swisstms.entitlements.internal;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.domain.common.AssetClass;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.marketdata.SubscriptionRequest.Level;
import ch.swisstms.domain.ports.EntitlementPort;
import ch.swisstms.domain.ports.EntitlementPort.DenyReason;
import ch.swisstms.domain.ports.EntitlementPort.EntitlementDecision.Denied;
import ch.swisstms.domain.ports.EntitlementPort.EntitlementDecision.Granted;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import org.springframework.stereotype.Service;

/**
 * In-process Implementierung des {@link EntitlementPort} für Phase 8.
 *
 * <p>Phase 14 (Multi-Region) lagert das Backend nach Redis (zentral pro Region) und syncs
 * Entitlements aus Bloomberg EMRS / Refinitiv DACS.
 */
@Service
public class InMemoryEntitlementService implements EntitlementPort {

  private final ConcurrentHashMap<String, Set<String>> subjectToInstruments =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<KillScope, KillSwitchState> killSwitches =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<KillScope, String> killTrippers = new ConcurrentHashMap<>();
  private final SubmissionPublisher<KillSwitchEvent> killEvents = new SubmissionPublisher<>();
  private final HashChainWriter audit;

  public InMemoryEntitlementService(HashChainWriter audit) {
    this.audit = audit;
    // Phase 8 — load demo entitlements
    subjectToInstruments.put("alice.trader", Set.of("CH0038863350:XSWX", "CH0012005267:XSWX"));
  }

  @Override
  public EntitlementDecision checkMarketData(
      String subjectId, InstrumentId instrument, Level level) {
    return checkOrderEntry(subjectId, instrument, AssetClass.EQUITY);
  }

  @Override
  public EntitlementDecision checkOrderEntry(
      String subjectId, InstrumentId instrument, AssetClass assetClass) {
    Set<String> instruments = subjectToInstruments.get(subjectId);
    if (instruments == null) {
      return new Denied(DenyReason.SUBJECT_NOT_FOUND);
    }
    String key = instrument.isin() + ":" + instrument.mic();
    return instruments.contains(key) ? new Granted() : new Denied(DenyReason.NOT_ENTITLED);
  }

  @Override
  public KillSwitchState killSwitchFor(KillScope scope) {
    return killSwitches.getOrDefault(scope, KillSwitchState.ARMED);
  }

  @Override
  public Flow.Publisher<KillSwitchEvent> killSwitchEvents() {
    return killEvents;
  }

  @Override
  public CompletionStage<Void> tripKillSwitch(
      KillScope scope, String tripperUserId, String reason) {
    killSwitches.put(scope, KillSwitchState.TRIPPED);
    killTrippers.put(scope, tripperUserId);
    killEvents.submit(new KillSwitchEvent(scope, KillSwitchState.TRIPPED, tripperUserId, reason));
    audit.append(
        ActorType.USER,
        tripperUserId,
        "killswitch.trip",
        "KillScope",
        scope.id(),
        (scope.type() + "/" + scope.id() + ":" + reason).getBytes(),
        null);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<Void> resetKillSwitch(KillScope scope, String resetterUserId) {
    // 4-eyes — must be different user from the tripper.
    String tripper = killTrippers.get(scope);
    if (resetterUserId.equals(tripper)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("4-eyes violated: tripper cannot reset"));
    }
    killSwitches.put(scope, KillSwitchState.RESET);
    killEvents.submit(
        new KillSwitchEvent(scope, KillSwitchState.RESET, resetterUserId, "manual reset"));
    audit.append(
        ActorType.USER,
        resetterUserId,
        "killswitch.reset",
        "KillScope",
        scope.id(),
        (scope.type() + "/" + scope.id()).getBytes(),
        null);
    return CompletableFuture.completedFuture(null);
  }

  public void grant(String subjectId, InstrumentId instrument) {
    subjectToInstruments
        .computeIfAbsent(subjectId, k -> ConcurrentHashMap.newKeySet())
        .add(instrument.isin() + ":" + instrument.mic());
  }

  public void revoke(String subjectId, InstrumentId instrument) {
    Set<String> set = subjectToInstruments.get(subjectId);
    if (set != null) set.remove(instrument.isin() + ":" + instrument.mic());
  }

  /** Demo-only: dump für REST-Endpoint. */
  public Map<String, Set<String>> dump() {
    return Map.copyOf(subjectToInstruments);
  }
}
