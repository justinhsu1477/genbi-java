package com.nlq.dto;

/**
 * WebSocket STATE 類型訊息的內容
 * Content of a STATE-type WebSocket message
 *
 * @param text   狀態描述 state description (e.g. "Generating SQL")
 * @param status 狀態 "start" or "end"
 */
public record StateContent(String text, String status) {}
