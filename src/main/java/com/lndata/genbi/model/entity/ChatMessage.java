package com.lndata.genbi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 聊天訊息 — 紀錄每次問答的完整內容
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_message_session", columnList = "session_id"),
        @Index(name = "idx_message_user", columnList = "user_id"),
        @Index(name = "idx_message_history", columnList = "user_id, session_id, profile_name, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage extends BaseEntity {

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "profile_name", nullable = false, length = 128)
    private String profileName;

    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(name = "query_rewrite", columnDefinition = "TEXT")
    private String queryRewrite;

    // 意圖類型 (normal_search, knowledge_search, agent_search, reject_search)
    @Column(name = "query_intent", length = 32)
    private String queryIntent;

    @Column(name = "sql_text", columnDefinition = "TEXT")
    private String sqlText;

    // 完整回答 JSON
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "model_id", length = 128)
    private String modelId;
}
