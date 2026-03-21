package com.nlq.dto;

import java.time.LocalDateTime;

/**
 * 聊天訊息回傳 DTO
 * Chat message response DTO for GET /qa/sessions/{sessionId}
 */
public record MessageResponse(
        Long id,
        String query,
        String queryRewrite,
        String queryIntent,
        String sqlText,
        String answer,
        String modelId,
        LocalDateTime createdAt
) {}
