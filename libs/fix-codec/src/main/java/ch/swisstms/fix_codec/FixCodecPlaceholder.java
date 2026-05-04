package ch.swisstms.fix_codec;

/**
 * Phase 2 — Platzhalter. Reale Implementierung folgt in Phase 3 (US1):
 *
 * <ul>
 *   <li>{@code session/SessionLifecycle} — wraps quickfix.Session
 *   <li>{@code store/JdbcMessageStore} — Postgres-backed (`fix_session_state`)
 *   <li>{@code application/MessageMapper} — domain ↔ FIX 4.4 / 5.0 SP2 dialects
 *   <li>{@code application/GapRecoveryHandler} — distinguishes app replay vs admin gap-fill
 * </ul>
 */
public final class FixCodecPlaceholder {
  private FixCodecPlaceholder() {}

  public static String version() {
    return "0.1.0-SNAPSHOT (Phase 2 placeholder)";
  }
}
