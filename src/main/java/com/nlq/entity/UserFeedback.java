package com.nlq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用戶回饋 — 建立後不可修改（upvote 會加入 RAG）
 */
@Entity
@Table(name = "user_feedbacks", indexes = {
        @Index(name = "idx_feedback_session", columnList = "session_id"),
        @Index(name = "idx_feedback_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "message_id")
    private Long messageId;

    // upvote / downvote
    @Column(name = "feedback_type", nullable = false, length = 16)
    private String feedbackType;

    @Column(name = "query", columnDefinition = "TEXT")
    private String query;

    @Column(name = "sql_text", columnDefinition = "TEXT")
    private String sqlText;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 沒有 setter — feedback 建立後不可修改
}
