package com.nlq.dto;

import java.time.LocalDateTime;

/**
 * Profile 回傳 DTO — 不包含密碼
 */
public record ProfileResponse(
        Long id,
        String profileName,
        String connName,
        String dbType,
        String dbUrl,
        String tablesInfo,
        String hints,
        String comments,
        Boolean rlsEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
