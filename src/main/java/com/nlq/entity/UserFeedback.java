package com.nlq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用戶回饋 — upvote 會加入 RAG 做為範例，downvote 只做記錄
 */
@Entity
@Table(name = "user_feedbacks", indexes = {
        @Index(name = "idx_feedback_session", columnList = "session_id"),
        @Index(name = "idx_feedback_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    // 對應的訊息 ID
    @Column(name = "message_id")
    private Long messageId;

    // upvote / downvote
    @Column(name = "feedback_type", nullable = false, length = 16)
    private String feedbackType;

    // 用戶的查詢
    @Column(name = "query", columnDefinition = "TEXT")
    private String query;

    // 對應的 SQL
    @Column(name = "sql_text", columnDefinition = "TEXT")
    private String sqlText;

    // 用戶備註
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
