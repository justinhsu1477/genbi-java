package com.lndata.genbi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用戶回饋 — upvote 會加入 RAG
 */
@Entity
@Table(name = "user_feedbacks", indexes = {
        @Index(name = "idx_feedback_session", columnList = "session_id"),
        @Index(name = "idx_feedback_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class UserFeedback extends BaseEntity {

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
}
