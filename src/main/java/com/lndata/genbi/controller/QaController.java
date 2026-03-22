package com.lndata.genbi.controller;

import com.lndata.genbi.config.BedrockProperties;
import com.lndata.genbi.dto.*;
import com.lndata.genbi.service.FeedbackService;
import com.lndata.genbi.service.ProfileService;
import com.lndata.genbi.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * QA REST Controller — GenBI 的 CRUD 端點
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

    /** 取得可用的 profile 和模型列表 */
    @GetMapping("/option")
    @Operation(summary = "取得選項 / Get options", description = "返回可用 profile 和模型列表")
    public ApiResponse<OptionResponse> getOption() {
        var profiles = profileService.getAllProfiles();
        List<String> profileNames = List.copyOf(profiles.keySet());

        // 目前支援的 Bedrock 模型
        List<String> models = List.of(
                "anthropic.claude-3-5-sonnet-20241022-v2:0",
                "anthropic.claude-3-sonnet-20240229-v1:0",
                "anthropic.claude-3-haiku-20240307-v1:0"
        );

        return ApiResponse.ok(new OptionResponse(profileNames, models, bedrockProperties.modelId()));
    }

    // ===== GET /qa/sessions =====

    /** 取得用戶的所有 session */
    @GetMapping("/sessions")
    @Operation(summary = "取得 Session 列表 / Get sessions")
    public ApiResponse<List<SessionResponse>> getSessions(@RequestParam String userId) {
        return ApiResponse.ok(sessionService.getSessionsByUser(userId));
    }

    // ===== GET /qa/sessions/{sessionId} =====

    /** 取得 session 內的所有訊息 */
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "取得 Session 訊息 / Get session messages")
    public ApiResponse<List<MessageResponse>> getSessionMessages(@PathVariable String sessionId) {
        return ApiResponse.ok(sessionService.getMessagesBySession(sessionId));
    }

    // ===== DELETE /qa/sessions/{sessionId} =====

    /** 刪除 session 及其所有訊息 */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "刪除 Session / Delete session")
    public ApiResponse<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return ApiResponse.ok();
    }

    // ===== POST /qa/feedback =====

    /** 提交用戶回饋（讚/踩） */
    @PostMapping("/feedback")
    @Operation(summary = "提交回饋 / Submit feedback")
    public ApiResponse<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.saveFeedback(request);
        return ApiResponse.ok();
    }
}
