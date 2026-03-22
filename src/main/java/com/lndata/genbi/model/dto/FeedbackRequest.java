package com.lndata.genbi.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用戶回饋請求 DTO
 */
public record FeedbackRequest(
        @NotBlank(message = "sessionId 不能為空 / sessionId is required")
        String sessionId,

        @NotBlank(message = "userId 不能為空 / userId is required")
        String userId,

        Long messageId,

        @NotBlank(message = "feedbackType 不能為空 / feedbackType is required")
        @Pattern(regexp = "upvote|downvote", message = "feedbackType 必須是 upvote 或 downvote")
        String feedbackType,

        String query,
        String sqlText,
        String comment
) {}
