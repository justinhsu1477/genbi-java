package com.lndata.genbi.dto;

import java.util.List;

/**
 * 選項回傳 DTO — profile 列表 + 模型列表
 */
public record OptionResponse(
        List<String> profiles,
        List<String> bedrockModelIds,
        String defaultModelId
) {}
