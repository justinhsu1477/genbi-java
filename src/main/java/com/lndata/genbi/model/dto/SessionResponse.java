package com.lndata.genbi.model.dto;

import java.time.Instant;

/**
 * Session 回傳 DTO
 */
public record SessionResponse(
        String sessionId,
        String userId,
        String profileName,
        String title,
        Instant createdAt,
        Instant updatedAt
) {}
