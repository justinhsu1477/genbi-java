package com.nlq.controller;

import com.nlq.config.BedrockProperties;
import com.nlq.dto.*;
import com.nlq.service.FeedbackService;
import com.nlq.service.ProfileService;
import com.nlq.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * QA REST Controller — GenBI 的 CRUD 端點
 * QA REST endpoints for GenBI
 *
 * 對應 Python: api/main.py 的 REST 路由
 * Maps to Python's api/main.py REST routes
 *
 * API 設計參照 docs/api-review.txt 的重構方案
 */
@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
@Tag(name = "QA", description = "GenBI 問答相關 API / GenBI Q&A APIs")
public class QaController {

    private final SessionService sessionService;
    private final FeedbackService feedbackService;
    private final ProfileService profileService;
    private final BedrockProperties bedrockProperties;

    // ===== GET /qa/option =====

    /**
     * 取得可用的 profile 和模型列表
     * Get available profiles and model list
     *
     * 對應 Python: /qa/option
     */
    @GetMapping("/option")
    @Operation(summary = "取得選項 / Get options", description = "返回可用 profile 和模型列表")
    public ApiResponse<OptionResponse> getOption() {
        var profiles = profileService.getAllProfiles();
        List<String> profileNames = List.copyOf(profiles.keySet());

        // 目前支援的 Bedrock 模型 currently supported models
        List<String> models = List.of(
                "anthropic.claude-3-5-sonnet-20241022-v2:0",
                "anthropic.claude-3-sonnet-20240229-v1:0",
                "anthropic.claude-3-haiku-20240307-v1:0"
        );

        return ApiResponse.ok(new OptionResponse(profileNames, models, bedrockProperties.modelId()));
    }

    // ===== GET /qa/sessions =====

    /**
     * 取得用戶的所有 session
     * Get all sessions for a user
     *
     * 對應 Python: /qa/get_sessions
     */
    @GetMapping("/sessions")
    @Operation(summary = "取得 Session 列表 / Get sessions")
    public ApiResponse<List<SessionResponse>> getSessions(@RequestParam String userId) {
        return ApiResponse.ok(sessionService.getSessionsByUser(userId));
    }

    // ===== GET /qa/sessions/{sessionId} =====

    /**
     * 取得 session 內的所有訊息
     * Get all messages in a session
     *
     * 對應 Python: /qa/get_history_by_session
     */
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "取得 Session 訊息 / Get session messages")
    public ApiResponse<List<MessageResponse>> getSessionMessages(@PathVariable String sessionId) {
        return ApiResponse.ok(sessionService.getMessagesBySession(sessionId));
    }

    // ===== DELETE /qa/sessions/{sessionId} =====

    /**
     * 刪除 session 及其所有訊息
     * Delete a session and all its messages
     *
     * 對應 Python: /qa/delete_history_by_session
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "刪除 Session / Delete session")
    public ApiResponse<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return ApiResponse.ok();
    }

    // ===== POST /qa/feedback =====

    /**
     * 提交用戶回饋（讚/踩）
     * Submit user feedback (upvote/downvote)
     *
     * 對應 Python: /qa/user_feedback
     */
    @PostMapping("/feedback")
    @Operation(summary = "提交回饋 / Submit feedback")
    public ApiResponse<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.saveFeedback(request);
        return ApiResponse.ok();
    }
}
