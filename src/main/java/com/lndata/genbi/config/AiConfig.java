package com.lndata.genbi.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * AI 服務配置 — 注入 Spring AI ChatClient（僅 qas/prod 環境）
 *
 * Spring AI 自動配置會根據 application.yml 建立 ChatModel（Bedrock Converse），
 * 這裡透過 ChatClient.Builder 建立 ChatClient Bean 供 Service 層注入。
 * 日後若要換 LLM 提供者（如 OpenAI），只需替換 starter dependency + yml 設定，
 * ChatClient 介面不變。
 */
@Configuration
@Profile("!dev")
public class AiConfig {

    /**
     * ChatClient Bean — 包裝 ChatModel，提供 Fluent API
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
