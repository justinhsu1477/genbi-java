package com.lndata.genbi.service;

import com.lndata.genbi.model.constant.PromptType;

import java.util.Map;

/**
 * Prompt 管理服務 — 從 prompt_map 提取模板並填入變數
 *
 * Python 對應: generate_prompt.py 中的 generate_*_prompt() 系列函數
 */
public interface PromptService {

    /**
     * 建構 system prompt
     *
     * @param promptMap  profile 的 prompt_map (可為 null，使用預設)
     * @param type       prompt 類型
     * @param modelId    模型 ID (用於 model-specific prompt 查找)
     * @param variables  替換變數 (key → value)
     * @return 完成變數替換後的 system prompt
     */
    String buildSystemPrompt(Map<String, Object> promptMap, PromptType type,
                             String modelId, Map<String, String> variables);

    /**
     * 建構 user prompt
     *
     * @param promptMap  profile 的 prompt_map (可為 null，使用預設)
     * @param type       prompt 類型
     * @param modelId    模型 ID
     * @param variables  替換變數 (key → value)
     * @return 完成變數替換後的 user prompt
     */
    String buildUserPrompt(Map<String, Object> promptMap, PromptType type,
                           String modelId, Map<String, String> variables);

    /**
     * 驗證 prompt 是否包含所有必要變數
     *
     * @return true 如果所有必要的 {variable} 都存在
     */
    boolean validatePrompt(String systemPrompt, String userPrompt, PromptType type);

    /**
     * 取得預設的完整 prompt_map — 用於初始化新 Profile
     */
    Map<String, Object> getDefaultPromptMap();
}
