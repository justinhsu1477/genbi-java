package com.nlq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS Bedrock 設定 — 從 application.yml 綁定
 */
@ConfigurationProperties(prefix = "nlq.bedrock")
public record BedrockProperties(
        String modelId,
        String region,
        int maxTokens,
        double temperature
) {
    // 預設值
    public BedrockProperties {
        if (modelId == null) modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";
        if (region == null) region = "us-west-2";
        if (maxTokens <= 0) maxTokens = 4096;
        if (temperature <= 0) temperature = 0.01;
    }
}
