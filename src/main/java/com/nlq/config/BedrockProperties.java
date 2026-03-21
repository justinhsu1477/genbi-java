package com.nlq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS Bedrock 設定 — 從 application.yml 綁定
 * AWS Bedrock configuration bound from application.yml
 *
 * 對應 Python: utils/env_var.py 的 BEDROCK_REGION, model_id 等
 * Maps to Python's env_var.py BEDROCK_REGION, model_id, etc.
 */
@ConfigurationProperties(prefix = "nlq.bedrock")
public record BedrockProperties(
        String modelId,
        String region,
        int maxTokens,
        double temperature
) {
    // 提供預設值 provide defaults
    public BedrockProperties {
        if (modelId == null) modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";
        if (region == null) region = "us-west-2";
        if (maxTokens <= 0) maxTokens = 4096;
        if (temperature <= 0) temperature = 0.01;
    }
}
