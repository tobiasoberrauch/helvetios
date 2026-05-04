-- QuickFIX/J JdbcStoreFactory backing table (used by libs/fix-codec).
-- Per-session sequence numbers persist here for crash recovery.

CREATE TABLE fix_session_state (
    sender_comp_id    TEXT NOT NULL,
    target_comp_id    TEXT NOT NULL,
    session_qualifier TEXT NOT NULL DEFAULT '',
    next_sender_seq   BIGINT NOT NULL,
    next_target_seq   BIGINT NOT NULL,
    last_logon        TIMESTAMPTZ,
    PRIMARY KEY (sender_comp_id, target_comp_id, session_qualifier)
);

CREATE TABLE fix_session_messages (
    sender_comp_id    TEXT NOT NULL,
    target_comp_id    TEXT NOT NULL,
    session_qualifier TEXT NOT NULL DEFAULT '',
    seq_num           BIGINT NOT NULL,
    message           TEXT NOT NULL,
    PRIMARY KEY (sender_comp_id, target_comp_id, session_qualifier, seq_num)
);
