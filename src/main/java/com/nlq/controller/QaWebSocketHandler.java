package com.nlq.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlq.dto.*;
import com.nlq.enums.ContentType;
import com.nlq.enums.QueryState;
import com.nlq.service.DatabaseService;
import com.nlq.service.LlmService;
import com.nlq.service.ProfileService;
import com.nlq.service.RetrievalService;
import com.nlq.statemachine.QueryStateMachine;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

/**
 * WebSocket Handler — 處理 /qa/ws 的所有訊息
 *
 * 流程:
 * 1. 前端透過 WebSocket 送 Question JSON
 * 2. 後端建立 StateMachine，逐步執行各狀態
 * 3. 每個狀態前後推送 STATE 訊息 (start/end) 讓前端顯示進度
 * 4. 最後推送 END 訊息，包含完整 Answer
 */
@Slf4j
@Component
@AllArgsConstructor
public class QaWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final LlmService llmService;
    private final DatabaseService databaseService;
    private final RetrievalService retrievalService;
    private final ProfileService profileService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message: {}", payload);

        Question question = objectMapper.readValue(payload, Question.class);
        String sessionId = question.sessionId();
        String userId = question.userId();

        try {
            Answer answer = processQuestion(session, question);
            // 送出最終結果
            sendMessage(session, sessionId, userId, ContentType.END, answer);
        } catch (Exception e) {
            log.error("Error processing question: {}", e.getMessage(), e);
            sendMessage(session, sessionId, userId, ContentType.EXCEPTION, e.getMessage());
        }
    }

    /** 處理查詢的主要邏輯 — 建立狀態機並逐步執行 */
    private Answer processQuestion(WebSocketSession session, Question question) throws Exception {
        String sessionId = question.sessionId();
        String userId = question.userId();

        // 1. 取得 database profile
        Map<String, Map<String, Object>> allProfiles = profileService.getAllProfiles();
        Map<String, Object> dbProfile = allProfiles.getOrDefault(question.profileName(), Map.of());

        // 2. 取得歷史查詢
        List<String> history = List.of();
        if (question.contextWindow() > 0) {
            history = profileService.getHistoryBySession(
                    question.profileName(), userId, sessionId, question.contextWindow());
        }

        // 3. 建立上下文和狀態機
        ProcessingContext context = ProcessingContext.from(question, dbProfile, history);
        QueryStateMachine stateMachine = new QueryStateMachine(context, llmService, databaseService, retrievalService);

        // 4. 逐狀態執行，每步推送進度
        while (!stateMachine.isTerminal()) {
            String stateLabel = getStateLabel(stateMachine.getState());

            // 推送 STATE start
            sendMessage(session, sessionId, userId, ContentType.STATE,
                    new StateContent(stateLabel, "start"));

            stateMachine.executeCurrentState();

            // 推送 STATE end
            sendMessage(session, sessionId, userId, ContentType.STATE,
                    new StateContent(stateLabel, "end"));
        }

        // 5. 建議問題
        if (question.genSuggestedQuestionFlag()
                && !"entity_select".equals(stateMachine.getAnswer().getQueryIntent())) {
            if (stateMachine.isSearchIntent() || stateMachine.isAgentIntent()) {
                sendMessage(session, sessionId, userId, ContentType.STATE,
                        new StateContent("Generating Suggested Questions", "start"));
                stateMachine.handleSuggestQuestion();
                sendMessage(session, sessionId, userId, ContentType.STATE,
                        new StateContent("Generating Suggested Questions", "end"));
            }
        }

        // 6. 數據可視化
        if (stateMachine.getState() == QueryState.COMPLETE) {
            sendMessage(session, sessionId, userId, ContentType.STATE,
                    new StateContent("Data Visualization", "start"));
            stateMachine.handleDataVisualization();
            sendMessage(session, sessionId, userId, ContentType.STATE,
                    new StateContent("Data Visualization", "end"));
        }

        return stateMachine.getAnswer();
    }

    /** 送出 WebSocket 訊息 */
    private void sendMessage(WebSocketSession session, String sessionId,
                             String userId, ContentType contentType, Object content) throws Exception {
        WsMessage msg = new WsMessage(sessionId, userId, contentType.getValue(), content);
        String json = objectMapper.writeValueAsString(msg);
        session.sendMessage(new TextMessage(json));
    }

    /** 狀態對應的前端顯示文字 */
    private String getStateLabel(QueryState state) {
        return switch (state) {
            case INITIAL -> "Query Rewrite";
            case INTENT_RECOGNITION -> "Query Intent Analyse";
            case REJECT_INTENT -> "Reject Intent";
            case KNOWLEDGE_SEARCH -> "Knowledge Search Intent";
            case ENTITY_RETRIEVAL -> "Entity Info Retrieval";
            case QA_RETRIEVAL -> "QA Info Retrieval";
            case SQL_GENERATION -> "Generating SQL";
            case EXECUTE_QUERY -> "Database SQL Execution";
            case ANALYZE_DATA -> "Generating Data Insights";
            case AGENT_TASK -> "Agent Task Split";
            case AGENT_SEARCH -> "Agent SQL Generating";
            case AGENT_DATA_SUMMARY -> "Generating Data Insights";
            case ASK_ENTITY_SELECT -> "Entity Selection";
            case USER_SELECT_ENTITY -> "User Entity Select";
            default -> state.name();
        };
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket disconnected: {} ({})", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", exception.getMessage(), exception);
    }
}
