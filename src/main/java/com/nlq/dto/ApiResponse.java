package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 統一 API 回傳格式 — 所有 REST endpoint 都用此包裝
 * Unified API response wrapper for all REST endpoints
 *
 * @param code    狀態碼 status code (200=success, 4xx/5xx=error)
 * @param message 訊息 human-readable message
 * @param data    資料 response payload (null when error)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        int code,
        String message,
        T data
) {
    // 成功 success
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "success", null);
    }

    // 業務錯誤 business error
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // 伺服器錯誤 server error
    public static <T> ApiResponse<T> serverError(String message) {
        return new ApiResponse<>(500, message, null);
    }

    // 參數錯誤 bad request
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }

    // 找不到資源 not found
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }
}
