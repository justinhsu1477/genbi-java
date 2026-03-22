package com.nlq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 聊天會話 — 一個 session 包含多筆 ChatMessage
 */
@Entity
@Table(name = "chat_sessions", indexes = {
        @Index(name = "idx_session_user", columnList = "user_id"),
        @Index(name = "idx_session_profile", columnList = "profile_name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 需要，但外部不該用
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 只給 Builder 用
@Builder
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "profile_name", nullable = false, length = 128)
    private String profileName;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Domain methods（取代 setter） ---

    /** 觸發 updatedAt 更新（JPA @PreUpdate 會自動設定時間） */
    public void touch() {
        // 只要 save() 就會觸發 @PreUpdate，不需要手動設定
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }
}
