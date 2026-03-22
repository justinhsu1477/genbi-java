package com.lndata.genbi.enums;

/**
 * WebSocket 訊息類型
 */
public enum ContentType {
    EXCEPTION("exception"),  // 異常
    COMMON("common"),        // 一般訊息
    STATE("state"),          // 狀態更新
    END("end");              // 結束（最終結果）

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
