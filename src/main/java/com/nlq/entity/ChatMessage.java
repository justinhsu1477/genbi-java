package com.nlq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 聊天訊息 — 紀錄每次問答的完整內容
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_message_session", columnList = "session_id"),
        @Index(name = "idx_message_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "profile_name", nullable = false, length = 128)
    private String profileName;

    // 用戶原始查詢
    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;

    // 改寫後的查詢
    @Column(name = "query_rewrite", columnDefinition = "TEXT")
    private String queryRewrite;

    // 意圖類型 (normal_search, knowledge_search, agent_search, reject_search)
    @Column(name = "query_intent", length = 32)
    private String queryIntent;

    // 生成的 SQL
    @Column(name = "sql_text", columnDefinition = "TEXT")
    private String sqlText;

    // 完整回答 JSON
    @Column(name = "answer", columnDefinition = "LONGTEXT")
    private String answer;

    // 使用的模型 ID
    @Column(name = "model_id", length = 128)
    private String modelId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
