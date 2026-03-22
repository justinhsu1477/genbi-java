package com.lndata.genbi.dto;

import java.time.LocalDateTime;

/**
 * Session 回傳 DTO
 */
public record SessionResponse(
        String sessionId,
        String userId,
        String profileName,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
