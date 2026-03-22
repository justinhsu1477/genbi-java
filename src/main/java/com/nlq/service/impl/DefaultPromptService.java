package com.nlq.service.impl;

import com.nlq.enums.PromptType;
import com.nlq.service.PromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Prompt 管理服務實作 — 從 prompt_map 提取模板並做變數替換
 *
 * Python 對應: generate_prompt.py 的 generate_llm_prompt()、generate_intent_prompt() 等
 *
 * 設計決策:
 * - Python 版本為每個 model_id 存一份 prompt (5 模型 × 9 類型 = 45 份)
 *   但實際上各模型的 prompt 內容幾乎相同
 * - Java 版簡化為：每種 type 一份預設模板，支援 prompt_map 覆蓋
 * - prompt_map 結構: {type_key: {system_prompt: "...", user_prompt: "..."}}
 *   或 Python 相容格式: {type_key: {system_prompt: {model_id: "..."}, user_prompt: {model_id: "..."}}}
 */
@Slf4j
@Service
public class DefaultPromptService implements PromptService {

    @Override
    public String buildSystemPrompt(Map<String, Object> promptMap, PromptType type,
                                    String modelId, Map<String, String> variables) {
        String template = extractTemplate(promptMap, type.getMapKey(), "system_prompt",
                modelId, type.getDefaultSystemPrompt());
        return replaceVariables(template, variables);
    }

    @Override
    public String buildUserPrompt(Map<String, Object> promptMap, PromptType type,
                                  String modelId, Map<String, String> variables) {
        String template = extractTemplate(promptMap, type.getMapKey(), "user_prompt",
                modelId, type.getDefaultUserPrompt());
        return replaceVariables(template, variables);
    }

    @Override
    public boolean validatePrompt(String systemPrompt, String userPrompt, PromptType type) {
        for (String var : type.getRequiredSystemVars()) {
            if (!systemPrompt.contains("{" + var + "}")) {
                log.warn("[Prompt] Missing system variable: {} in type={}", var, type.getMapKey());
                return false;
            }
        }
        for (String var : type.getRequiredUserVars()) {
            if (!userPrompt.contains("{" + var + "}")) {
                log.warn("[Prompt] Missing user variable: {} in type={}", var, type.getMapKey());
                return false;
            }
        }
        return true;
    }

    @Override
    public Map<String, Object> getDefaultPromptMap() {
        return PromptType.buildDefaultPromptMap();
    }

    // --- 內部方法 ---

    /**
     * 從 prompt_map 提取模板 — 支援兩種格式:
     * 1. 簡化格式: {type: {system_prompt: "template"}}
     * 2. Python 相容格式: {type: {system_prompt: {model_id: "template"}}}
     */
    @SuppressWarnings("unchecked")
    String extractTemplate(Map<String, Object> promptMap, String typeKey,
                                   String promptKey, String modelId, String defaultTemplate) {
        if (promptMap == null) return defaultTemplate;

        Object typeSection = promptMap.get(typeKey);
        if (typeSection == null) return defaultTemplate;

        if (typeSection instanceof Map<?, ?> typeMap) {
            Object promptValue = typeMap.get(promptKey);
            if (promptValue == null) return defaultTemplate;

            // 格式 1: 直接是字串
            if (promptValue instanceof String str) {
                return str;
            }

            // 格式 2: 巢狀 Map {model_id: "template"}
            if (promptValue instanceof Map<?, ?> modelMap) {
                // 先用完整 model_id 查
                String resolved = resolveModelKey(modelId);
                Object value = modelMap.get(resolved);
                if (value instanceof String str) return str;

                // fallback: 取第一個非空值
                for (Object v : modelMap.values()) {
                    if (v instanceof String str && !str.isBlank()) return str;
                }
            }
        }

        return defaultTemplate;
    }

    /**
     * 變數替換 — 將 {key} 替換為 value
     *
     * 注意: 使用簡單字串替換，不用正則（避免 prompt 中的特殊字元干擾）
     */
    String replaceVariables(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 將完整 Bedrock model ID 轉為 prompt_map 中的短名稱
     *
     * Python 對應: support_model_ids_map
     */
    private String resolveModelKey(String modelId) {
        if (modelId == null) return "sonnet-3-5-20240620v1-0";
        return switch (modelId) {
            case "anthropic.claude-3-haiku-20240307-v1:0" -> "haiku-20240307v1-0";
            case "anthropic.claude-3-sonnet-20240229-v1:0" -> "sonnet-20240229v1-0";
            case "anthropic.claude-3-5-sonnet-20240620-v1:0" -> "sonnet-3-5-20240620v1-0";
            case "mistral.mixtral-8x7b-instruct-v0:1" -> "mixtral-8x7b-instruct-0";
            case "meta.llama3-70b-instruct-v1:0" -> "llama3-70b-instruct-0";
            default -> modelId; // 已經是短名稱或自訂 model
        };
    }
}
