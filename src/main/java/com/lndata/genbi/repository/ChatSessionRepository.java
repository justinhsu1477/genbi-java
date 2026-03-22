package com.lndata.genbi.repository;

import com.lndata.genbi.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天會話 Repository
 */
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 依用戶查所有 session（按更新時間倒序）
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<ChatSession> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
