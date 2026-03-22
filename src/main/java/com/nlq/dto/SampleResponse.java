package com.nlq.dto;

import java.util.Map;

/**
 * 範例資料回傳格式
 *
 * @param id     OpenSearch 文件 ID
 * @param source 文件內容 (不含 vector_field)
 */
public record SampleResponse(
        String id,
        Map<String, Object> source
) {}
