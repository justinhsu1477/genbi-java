package com.lndata.genbi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 聊天會話 — 一個 session 包含多筆 ChatMessage
 */
@Entity
@Table(name = "chat_sessions", indexes = {
        @Index(name = "idx_session_user", columnList = "user_id"),
        @Index(name = "idx_session_profile", columnList = "profile_name")
})
@Getter
@Setter
@NoArgsConstructor
public class ChatSession extends BaseEntity {

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "profile_name", nullable = false, length = 128)
    private String profileName;

    @Column(name = "title", length = 256)
    private String title;

    // --- Domain methods ---

    /** 觸發 updatedAt 更新（BaseEntity @LastModifiedDate 會自動設定時間） */
    public void touch() {
        // 只要 save() 就會觸發 @LastModifiedDate，不需要手動設定
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }
}
