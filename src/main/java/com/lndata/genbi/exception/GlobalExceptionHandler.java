package com.lndata.genbi.exception;

import com.lndata.genbi.model.response.BaseRestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全域例外處理 — 統一攔截所有 Controller 異常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 業務邏輯異常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseRestResponse> handleBusinessException(BusinessException e) {
        log.warn("[Business] code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getCode())
                .body(BaseRestResponse.failure(e.getMessage()));
    }

    /** 參數驗證失敗 (@Valid) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseRestResponse> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[Validation] {}", details);
        return ResponseEntity
                .badRequest()
                .body(BaseRestResponse.failure(details));
    }

    /** 缺少必要參數 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseRestResponse> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[MissingParam] {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(BaseRestResponse.failure("Missing parameter: " + e.getParameterName()));
    }

    /** 兜底：未預期的異常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseRestResponse> handleAll(Exception e) {
        log.error("[Unexpected] {}", e.getMessage(), e);
        return ResponseEntity
                .internalServerError()
                .body(BaseRestResponse.failure("Internal server error"));
    }
}
