package com.lndata.genbi.service;

import com.lndata.genbi.model.dto.MessageResponse;
import com.lndata.genbi.model.dto.SessionResponse;
import com.lndata.genbi.model.entity.ChatMessage;
import com.lndata.genbi.model.entity.ChatSession;
import com.lndata.genbi.exception.BusinessException;
import com.lndata.genbi.repository.ChatMessageRepository;
import com.lndata.genbi.repository.ChatSessionRepository;
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
                .orElseGet(() -> {
                    ChatSession newSession = new ChatSession();
                    newSession.setSessionId(sessionId);
                    newSession.setUserId(userId);
                    newSession.setProfileName(profileName);
                    newSession.setTitle(query.length() > 50 ? query.substring(0, 50) + "..." : query);
                    return sessionRepository.save(newSession);
                });

        // 觸發 @LastModifiedDate 更新 updatedAt
        session.touch();
        sessionRepository.save(session);

        // 儲存訊息
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
        msg.setProfileName(profileName);
        msg.setQuery(query);
        msg.setQueryRewrite(queryRewrite);
        msg.setQueryIntent(queryIntent);
        msg.setSqlText(sqlText);
        msg.setAnswer(answerJson);
        msg.setModelId(modelId);
        messageRepository.save(msg);
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
