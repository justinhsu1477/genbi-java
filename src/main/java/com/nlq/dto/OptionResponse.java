package com.nlq.dto;

import java.util.List;

/**
 * 選項回傳 DTO — profile 列表 + 模型列表
 * Option response for GET /qa/option
 *
 * 對應 Python: /qa/option endpoint
 */
public record OptionResponse(
        List<String> profiles,
        List<String> bedrockModelIds,
        String defaultModelId
) {}
