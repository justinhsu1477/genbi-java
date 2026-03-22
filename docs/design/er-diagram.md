# GenBI Entity-Relationship Diagram / GenBI 實體關係圖

## ER Diagram (ASCII)

```
┌──────────────────────────────────────┐
│           chat_sessions              │
├──────────────────────────────────────┤
│ PK  id            BIGINT AUTO_INCR  │
│ UQ  session_id    VARCHAR(64)       │
│     user_id       VARCHAR(64)   NN  │
│     profile_name  VARCHAR(128)  NN  │
│     title         VARCHAR(256)      │
│     created_at    DATETIME      NN  │
│     updated_at    DATETIME          │
├──────────────────────────────────────┤
│ IDX idx_session_user    (user_id)   │
│ IDX idx_session_profile (profile)   │
└─────────────┬────────────────────────┘
              │ 1
              │
              │ session_id
              │
              │ N
┌─────────────┴────────────────────────┐
│           chat_messages              │
├──────────────────────────────────────┤
│ PK  id            BIGINT AUTO_INCR  │
│ FK  session_id    VARCHAR(64)   NN  │──→ chat_sessions.session_id
│     user_id       VARCHAR(64)   NN  │
│     profile_name  VARCHAR(128)  NN  │
│     query         TEXT          NN  │  ← 用戶原始問題 original query
│     query_rewrite TEXT              │  ← 改寫後的問題 rewritten query
│     query_intent  VARCHAR(32)       │  ← normal/knowledge/agent/reject
│     sql_text      TEXT              │  ← LLM 生成的 SQL
│     answer        LONGTEXT          │  ← 完整回答 JSON
│     model_id      VARCHAR(128)      │  ← 使用的 LLM 模型
│     created_at    DATETIME      NN  │
├──────────────────────────────────────┤
│ IDX idx_message_session (session_id)│
│ IDX idx_message_user    (user_id)   │
└─────────────┬────────────────────────┘
              │ 1
              │
              │ message_id (nullable)
              │
              │ N
┌─────────────┴────────────────────────┐
│          user_feedbacks              │
├──────────────────────────────────────┤
│ PK  id            BIGINT AUTO_INCR  │
│ FK  session_id    VARCHAR(64)   NN  │──→ chat_sessions.session_id
│     user_id       VARCHAR(64)   NN  │
│ FK  message_id    BIGINT            │──→ chat_messages.id (nullable)
│     feedback_type VARCHAR(16)   NN  │  ← upvote / downvote
│     query         TEXT              │  ← 當時的查詢
│     sql_text      TEXT              │  ← 當時的 SQL
│     comment       TEXT              │  ← 用戶備註
│     created_at    DATETIME      NN  │
├──────────────────────────────────────┤
│ IDX idx_feedback_session (session_id)│
│ IDX idx_feedback_user    (user_id)  │
└──────────────────────────────────────┘
```

## Relationships / 關聯

```
chat_sessions  1 ──→ N  chat_messages     一個 Session 有多筆問答
chat_sessions  1 ──→ N  user_feedbacks    一個 Session 有多筆回饋
chat_messages  1 ──→ N  user_feedbacks    一筆訊息可有多筆回饋 (nullable)
```

## Data Flow / 資料流

```
用戶發問 (WebSocket /qa/ws)
    │
    ▼
┌─────────────┐    auto-create if not exist
│ chat_sessions│ ◄─── SessionService.saveMessage()
└──────┬──────┘
       │
       ▼
┌──────────────┐
│ chat_messages │ ◄─── 每次問答結束後儲存 saved after each Q&A
└──────┬───────┘
       │
       ▼  (用戶按讚/踩 user upvote/downvote)
┌───────────────┐
│ user_feedbacks │ ◄─── POST /qa/feedback
└───────────────┘
       │
       ▼  (未來 future)
   upvote → 加入 RAG 知識庫 (OpenSearch)
   downvote → 記錄供分析 logged for analysis
```

## Future Entities (產品化需要的表) / 未來擴展

```
┌──────────────────────────────────────┐
│         db_profiles (未來)            │
├──────────────────────────────────────┤
│ PK  id            BIGINT AUTO_INCR  │
│     profile_name  VARCHAR(128)  UQ  │
│     db_type       VARCHAR(16)   NN  │  ← mysql / oracle / postgresql
│     db_url        VARCHAR(512)  NN  │
│     db_username   VARCHAR(128)      │
│     db_password   VARCHAR(256)      │  ← 加密儲存 encrypted
│     tables_info   LONGTEXT          │  ← DDL schema
│     hints         TEXT              │  ← 業務邏輯提示
│     prompt_map    JSON              │  ← 客製 prompt
│     created_at    DATETIME      NN  │
│     updated_at    DATETIME          │
├──────────────────────────────────────┤
│ 目前: MockProfileService 寫死       │
│ 未來: 從這張表讀取，管理後台可編輯   │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│         rag_examples (未來)           │
├──────────────────────────────────────┤
│ PK  id            BIGINT AUTO_INCR  │
│ FK  profile_name  VARCHAR(128)  NN  │──→ db_profiles.profile_name
│     question      TEXT          NN  │  ← 範例問題
│     sql_text      TEXT          NN  │  ← 對應 SQL
│     source        VARCHAR(16)       │  ← manual / upvote / import
│     embedding_id  VARCHAR(128)      │  ← OpenSearch vector ID
│     created_at    DATETIME      NN  │
├──────────────────────────────────────┤
│ upvote 的問答會自動加入這裡          │
│ 同時寫入 OpenSearch 做向量檢索       │
└──────────────────────────────────────┘
```

## Complete ER (Current + Future)

```
                    ┌───────────────┐
                    │  db_profiles  │ (未來)
                    │  Profile 設定  │
                    └───────┬───────┘
                            │ 1
              ┌─────────────┼──────────────┐
              │ N           │ N             │ N
    ┌─────────┴───┐  ┌──────┴──────┐  ┌────┴──────────┐
    │chat_sessions│  │rag_examples │  │ (OpenSearch)  │
    │  聊天會話    │  │ RAG 範例     │  │  向量檢索     │
    └──────┬──────┘  └─────────────┘  └───────────────┘
           │ 1                              (未來)
           │
     ┌─────┴──────┐
     │ N           │ N
┌────┴────────┐ ┌──┴────────────┐
│chat_messages│ │user_feedbacks │
│  問答紀錄    │ │  用戶回饋      │
└─────────────┘ └───────────────┘
```

## Notes / 備註

1. **目前狀態**: 3 張表 (sessions, messages, feedbacks) 已由 JPA 自動建表
2. **Profile 目前是 Mock**: `MockProfileService` 回傳寫死資料，未來改為 DB 讀取
3. **多租戶考量**: 所有表都有 `user_id`，未來可加 `tenant_id` 做多租戶隔離
4. **session_id 是業務 ID**: 前端生成 UUID，非 DB 自增 ID，方便 WebSocket 傳遞
5. **Oracle 相容**: 所有欄位長度和類型都兼容 MySQL + Oracle
