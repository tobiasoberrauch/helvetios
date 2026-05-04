package ch.swisstms.reconciler;

/**
 * Resultate des Reconcilers.
 *
 * <p>Bei Disagreements gewinnt {@code DROP_COPY_AUTHORITATIVE} — Drop-Copy ist die Source-of-Truth
 * (Constitution Principle V). Mismatches werden auf `warm.recon.mismatch.v1` publiziert;
 * übereinstimmende Paare werden zu authoritativen Fills auf `cold.exec.fill.v1` konsolidiert.
 */
public sealed interface ReconciliationDecision {
  JoinKey key();

  /** Beide Streams stimmen überein — Reconciler emittiert authoritativen Fill. */
  record Match(JoinKey key, String venueId, String quantity, String price)
      implements ReconciliationDecision {}

  /** Drop-Copy hat den Fill, OMS nicht — OMS muss nachgezogen werden. */
  record DropCopyOnly(JoinKey key, String venueId, String quantity, String price)
      implements ReconciliationDecision {}

  /** OMS hat den Fill, Drop-Copy nicht — wahrscheinlich Phantom oder verzögerte Drop-Copy. */
  record OmsOnly(JoinKey key) implements ReconciliationDecision {}

  /** Beide Streams haben den Fill, aber unterschiedliche Daten — Drop-Copy gewinnt. */
  record FieldMismatch(JoinKey key, String field, String omsValue, String dropCopyValue)
      implements ReconciliationDecision {}
}
