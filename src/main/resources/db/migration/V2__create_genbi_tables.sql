-- =====================================================
-- V2: GenBI 核心表 — db_profiles, chat_sessions, chat_messages, user_feedbacks
-- =====================================================

-- DB Profile — 每個 Profile 代表一組 DB 連線 + DDL + Hints
CREATE TABLE db_profiles
(
    id           BIGSERIAL PRIMARY KEY,
    profile_name VARCHAR(128)             NOT NULL UNIQUE,
    conn_name    VARCHAR(128),
    db_type      VARCHAR(32)              NOT NULL,
    db_url       VARCHAR(512)             NOT NULL,
    db_username  VARCHAR(128),
    db_password  VARCHAR(256),
    tables_info  TEXT,
    hints        TEXT,
    comments     TEXT,
    prompt_map   TEXT,
    rls_enabled  BOOLEAN                  DEFAULT FALSE,
    rls_config   TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_profile_name ON db_profiles (profile_name);

-- Chat Session — 一個 session 包含多筆 ChatMessage
CREATE TABLE chat_sessions
(
    id           BIGSERIAL PRIMARY KEY,
    session_id   VARCHAR(64)              NOT NULL UNIQUE,
    user_id      VARCHAR(64)              NOT NULL,
    profile_name VARCHAR(128)             NOT NULL,
    title        VARCHAR(256),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_session_user ON chat_sessions (user_id);
CREATE INDEX idx_session_profile ON chat_sessions (profile_name);

-- Chat Message — 紀錄每次問答的完整內容
CREATE TABLE chat_messages
(
    id            BIGSERIAL PRIMARY KEY,
    session_id    VARCHAR(64)              NOT NULL,
    user_id       VARCHAR(64)              NOT NULL,
    profile_name  VARCHAR(128)             NOT NULL,
    query         TEXT                     NOT NULL,
    query_rewrite TEXT,
    query_intent  VARCHAR(32),
    sql_text      TEXT,
    answer        TEXT,
    model_id      VARCHAR(128),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_message_session ON chat_messages (session_id);
CREATE INDEX idx_message_user ON chat_messages (user_id);
CREATE INDEX idx_message_history ON chat_messages (user_id, session_id, profile_name, created_at);

-- User Feedback — 用戶回饋（讚/踩）
CREATE TABLE user_feedbacks
(
    id            BIGSERIAL PRIMARY KEY,
    session_id    VARCHAR(64)              NOT NULL,
    user_id       VARCHAR(64)              NOT NULL,
    message_id    BIGINT,
    feedback_type VARCHAR(16)              NOT NULL,
    query         TEXT,
    sql_text      TEXT,
    comment       TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feedback_session ON user_feedbacks (session_id);
CREATE INDEX idx_feedback_user ON user_feedbacks (user_id);
