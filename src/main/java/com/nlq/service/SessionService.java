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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    /** 查詢用戶的所有 session */
    public List<SessionResponse> getSessionsByUser(String userId) {
        log.info("[Session] getSessionsByUser: userId={}", userId);
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    /** 查詢 session 內的所有訊息 */
    public List<MessageResponse> getMessagesBySession(String sessionId) {
        log.info("[Session] getMessagesBySession: sessionId={}", sessionId);
        sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BusinessException.notFound("Session " + sessionId));

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    /** 刪除 session 及其所有訊息 */
    @Transactional
    public void deleteSession(String sessionId) {
        log.info("[Session] deleteSession: sessionId={}", sessionId);
        sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BusinessException.notFound("Session " + sessionId));

        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteBySessionId(sessionId);
    }

    /** 儲存一筆問答訊息（由 WebSocket handler 呼叫） */
    @Transactional
    public void saveMessage(String sessionId, String userId, String profileName,
                            String query, String queryRewrite, String queryIntent,
                            String sqlText, String answerJson, String modelId) {
        // 確保 session 存在，不存在就建立
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseGet(() -> sessionRepository.save(ChatSession.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .profileName(profileName)
                        .title(query.length() > 50 ? query.substring(0, 50) + "..." : query)
                        .build()));

        // 觸發 @PreUpdate 更新 updatedAt
        session.touch();
        sessionRepository.save(session);

        // 儲存訊息
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

    // --- 轉換方法 ---

    private SessionResponse toSessionResponse(ChatSession s) {
        return new SessionResponse(s.getSessionId(), s.getUserId(), s.getProfileName(),
                s.getTitle(), s.getCreatedAt(), s.getUpdatedAt());
    }

    private MessageResponse toMessageResponse(ChatMessage m) {
        return new MessageResponse(m.getId(), m.getQuery(), m.getQueryRewrite(),
                m.getQueryIntent(), m.getSqlText(), m.getAnswer(), m.getModelId(), m.getCreatedAt());
    }
}
