package com.nlq.exception;

import lombok.Getter;

/**
 * 業務邏輯異常 — 用於可預期的業務錯誤（如 profile 不存在、session 找不到等）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    // 常用工廠方法
    public static BusinessException notFound(String resource) {
        return new BusinessException(404, resource + " not found");
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }
}
