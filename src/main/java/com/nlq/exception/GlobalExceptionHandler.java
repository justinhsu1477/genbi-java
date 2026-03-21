package com.nlq.exception;

import com.nlq.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全域例外處理 — 統一攔截所有 Controller 異常
 * Global exception handler — catches all controller exceptions uniformly
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 業務邏輯異常 Business exception
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("[Business] code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getCode())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 參數驗證失敗 Bean Validation errors (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[Validation] {}", details);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.badRequest(details));
    }

    /**
     * 缺少必要參數 Missing request parameter
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[MissingParam] {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.badRequest("Missing parameter: " + e.getParameterName()));
    }

    /**
     * 兜底：未預期的異常 Fallback for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception e) {
        log.error("[Unexpected] {}", e.getMessage(), e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.serverError("Internal server error"));
    }
}
