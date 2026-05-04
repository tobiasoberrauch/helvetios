package ch.swisstms.domain.ports;

import ch.swisstms.domain.common.AssetClass;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.marketdata.SubscriptionRequest.Level;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * Entitlements + kill-switch port. Cached in-process; refreshed via Kafka
 * `warm.entitlements.limit-update.v1`.
 */
public interface EntitlementPort {

  EntitlementDecision checkMarketData(String subjectId, InstrumentId instrument, Level level);

  EntitlementDecision checkOrderEntry(
      String subjectId, InstrumentId instrument, AssetClass assetClass);

  KillSwitchState killSwitchFor(KillScope scope);

  Flow.Publisher<KillSwitchEvent> killSwitchEvents();

  CompletionStage<Void> tripKillSwitch(KillScope scope, String tripperUserId, String reason);

  /** 4-eyes: must be different user from the tripper. */
  CompletionStage<Void> resetKillSwitch(KillScope scope, String resetterUserId);

  sealed interface EntitlementDecision {
    record Granted() implements EntitlementDecision {}

    record Denied(DenyReason reason) implements EntitlementDecision {}
  }

  enum DenyReason {
    NOT_ENTITLED,
    ENTITLEMENT_SOURCE_UNAVAILABLE,
    SUBJECT_NOT_FOUND,
    REVOKED,
    EXPIRED,
    KILL_SWITCH_TRIPPED
  }

  enum KillScopeType {
    TRADER,
    STRATEGY,
    DESK,
    CLIENT
  }

  record KillScope(KillScopeType type, String id) {}

  enum KillSwitchState {
    ARMED,
    TRIPPED,
    RESET
  }

  record KillSwitchEvent(
      KillScope scope, KillSwitchState newState, String tripperOrResetterUserId, String reason) {}
}
