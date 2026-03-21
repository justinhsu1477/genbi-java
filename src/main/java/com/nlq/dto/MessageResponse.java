package com.nlq.dto;

import java.time.LocalDateTime;

/**
 * 聊天訊息回傳 DTO
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
