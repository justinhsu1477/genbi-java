package com.nlq.repository;

import com.nlq.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 聊天訊息 Repository
 * Chat message data access
 *
 * 對應 Python: /qa/get_history_by_session, LogManagement.add_log()
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 依 session 查所有訊息（按建立時間排序）
    // Find all messages in a session, ordered by creation time
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // 依 session 刪除所有訊息
    // Delete all messages in a session
    void deleteBySessionId(String sessionId);

    // 依用戶 + profile 查最近 N 筆歷史（for context window）
    // Find recent N messages by user + profile (for conversation history)
    List<ChatMessage> findTop10ByUserIdAndProfileNameOrderByCreatedAtDesc(String userId, String profileName);
}
