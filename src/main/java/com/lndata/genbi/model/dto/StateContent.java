package com.lndata.genbi.model.dto;

/**
 * WebSocket STATE 類型訊息的內容
 *
 * @param text   狀態描述
 * @param status "start" 或 "end"
 */
public record StateContent(String text, String status) {}
