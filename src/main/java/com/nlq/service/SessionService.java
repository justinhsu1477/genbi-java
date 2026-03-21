package com.nlq.service;

import com.nlq.dto.MessageResponse;
import com.nlq.dto.SessionResponse;
import com.nlq.entity.ChatMessage;
import com.nlq.entity.ChatSession;
import com.nlq.exception.BusinessException;
import com.nlq.repository.ChatMessageRepository;
import com.nlq.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 會話管理服務 — Session CRUD
 * Session management service
 *
 * 對應 Python: /qa/get_sessions, /qa/get_history_by_session, /qa/delete_history_by_session
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    /**
     * 查詢用戶的所有 session
     * Get all sessions for a user
     */
    public List<SessionResponse> getSessionsByUser(String userId) {
        log.info("[Session] getSessionsByUser: userId={}", userId);
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    /**
     * 查詢 session 內的所有訊息
     * Get all messages in a session
     */
    public List<MessageResponse> getMessagesBySession(String sessionId) {
        log.info("[Session] getMessagesBySession: sessionId={}", sessionId);
        sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BusinessException.notFound("Session " + sessionId));

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    /**
     * 刪除 session 及其所有訊息
     * Delete a session and all its messages
     */
    @Transactional
    public void deleteSession(String sessionId) {
        log.info("[Session] deleteSession: sessionId={}", sessionId);
        sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BusinessException.notFound("Session " + sessionId));

        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteBySessionId(sessionId);
    }

    /**
     * 儲存一筆問答訊息（由 WebSocket handler 呼叫）
     * Save a Q&A message (called by WebSocket handler after query completes)
     */
    @Transactional
    public void saveMessage(String sessionId, String userId, String profileName,
                            String query, String queryRewrite, String queryIntent,
                            String sqlText, String answerJson, String modelId) {
        // 確保 session 存在，不存在就建立 ensure session exists
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseGet(() -> sessionRepository.save(ChatSession.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .profileName(profileName)
                        .title(query.length() > 50 ? query.substring(0, 50) + "..." : query)
                        .build()));

        // 更新 session 的 updatedAt update session timestamp
        session.setUpdatedAt(java.time.LocalDateTime.now());
        sessionRepository.save(session);

        // 儲存訊息 save message
        messageRepository.save(ChatMessage.builder()
                .sessionId(sessionId)
                .userId(userId)
                .profileName(profileName)
                .query(query)
                .queryRewrite(queryRewrite)
                .queryIntent(queryIntent)
                .sqlText(sqlText)
                .answer(answerJson)
                .modelId(modelId)
                .build());
    }

    // --- 轉換方法 Mapping methods ---

    private SessionResponse toSessionResponse(ChatSession s) {
        return new SessionResponse(s.getSessionId(), s.getUserId(), s.getProfileName(),
                s.getTitle(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private MessageResponse toMessageResponse(ChatMessage m) {
        return new MessageResponse(m.getId(), m.getQuery(), m.getQueryRewrite(),
                m.getQueryIntent(), m.getSqlText(), m.getAnswer(), m.getModelId(), m.getCreatedAt());
    }
}
