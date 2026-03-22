package com.lndata.genbi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket 訊息格式（送給前端）
 *
 * @param sessionId   會話 ID
 * @param userId      用戶 ID
 * @param contentType 訊息類型 (state/end/exception/common)
 * @param content     訊息內容
 */
public record WsMessage(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("user_id") String userId,
        @JsonProperty("content_type") String contentType,
        Object content
) {}
