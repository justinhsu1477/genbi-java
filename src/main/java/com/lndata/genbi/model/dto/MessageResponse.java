package com.lndata.genbi.model.dto;

import java.time.Instant;

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
        Instant createdAt
) {}
