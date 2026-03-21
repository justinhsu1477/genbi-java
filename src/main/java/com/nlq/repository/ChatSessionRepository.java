package com.nlq.repository;

import com.nlq.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天會話 Repository
 * Chat session data access
 *
 * 對應 Python: /qa/get_sessions, /qa/delete_history_by_session
 */
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 依用戶查所有 session（按更新時間倒序）
    // Find all sessions by user, ordered by latest update
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    // 依 sessionId 查詢
    Optional<ChatSession> findBySessionId(String sessionId);

    // 依 sessionId 刪除
    void deleteBySessionId(String sessionId);
}
