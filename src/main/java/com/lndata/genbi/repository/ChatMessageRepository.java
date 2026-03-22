package com.lndata.genbi.repository;

import com.lndata.genbi.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 聊天訊息 Repository
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 依 session 查所有訊息（按建立時間排序）
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // 依 session 刪除所有訊息
    void deleteBySessionId(String sessionId);

    // 依用戶 + profile 查最近 N 筆歷史（for context window）
    List<ChatMessage> findTop10ByUserIdAndProfileNameOrderByCreatedAtDesc(String userId, String profileName);
}
