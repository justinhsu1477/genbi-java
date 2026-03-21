package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket 訊息格式 (送給前端)
 * WebSocket message format sent to frontend
 *
 * @param sessionId   會話 ID session identifier
 * @param userId      用戶 ID user identifier
 * @param contentType 訊息類型 message type (state/end/exception/common)
 * @param content     訊息內容 message content (varies by type)
 */
public record WsMessage(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("user_id") String userId,
        @JsonProperty("content_type") String contentType,
        Object content
) {}
