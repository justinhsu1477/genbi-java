package com.lndata.genbi.model.dto;

import java.time.Instant;

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
        String promptMap,
        Boolean rlsEnabled,
        Instant createdAt,
        Instant updatedAt
) {}
