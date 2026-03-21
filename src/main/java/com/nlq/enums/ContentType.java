package com.nlq.enums;

/**
 * WebSocket 訊息類型
 * WebSocket message content type
 */
public enum ContentType {
    EXCEPTION("exception"),  // 異常 error message
    COMMON("common"),        // 一般訊息 normal message
    STATE("state"),          // 狀態更新 state progress update
    END("end");              // 結束 final result

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
