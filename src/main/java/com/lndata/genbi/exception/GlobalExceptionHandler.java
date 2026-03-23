package com.lndata.genbi.exception;

import com.lndata.genbi.model.response.BaseRestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /** 路徑參數型別錯誤 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseRestResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[TypeMismatch] {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(BaseRestResponse.failure("Invalid parameter type: " + e.getName()));
    }

    /** 請求體格式錯誤 (JSON parse error) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseRestResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("[NotReadable] {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(BaseRestResponse.failure("Malformed request body"));
    }

    /** 資源不存在 (404) */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseRestResponse> handleNotFound(NoResourceFoundException e) {
        log.warn("[NotFound] {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(BaseRestResponse.failure("Resource not found"));
    }

    /** 不支援的 HTTP 方法 (405) */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseRestResponse> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotSupported] {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(BaseRestResponse.failure("Method not allowed: " + e.getMethod()));
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
