package com.lndata.genbi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * 用戶提問請求
 *
 * @param query              用戶的自然語言問題
 * @param bedrockModelId     LLM 模型 ID
 * @param profileName        資料庫 profile 名稱
 * @param sessionId          會話 ID
 * @param userId             用戶 ID
 * @param username           用戶名稱
 * @param useRagFlag         是否使用 RAG
 * @param intentNerRecognitionFlag 是否做意圖識別
 * @param agentCotFlag       是否啟用 Agent 模式
 * @param explainGenProcessFlag 是否解釋生成過程
 * @param genSuggestedQuestionFlag 是否生成建議問題
 * @param answerWithInsights 是否附帶數據分析
 * @param contextWindow      上下文窗口大小
 * @param queryRewrite       改寫後的查詢
 * @param previousIntent     前一次的意圖
 * @param entityUserSelect   用戶選擇的實體
 * @param entityRetrieval    檢索到的實體
 */
public record Question(
        String query,

        @JsonProperty("bedrock_model_id")
        String bedrockModelId,

        @JsonProperty("profile_name")
        String profileName,

        @JsonProperty("session_id")
        String sessionId,

        @JsonProperty("user_id")
        String userId,

        String username,

        @JsonProperty("use_rag_flag")
        boolean useRagFlag,

        @JsonProperty("intent_ner_recognition_flag")
        boolean intentNerRecognitionFlag,

        @JsonProperty("agent_cot_flag")
        boolean agentCotFlag,

        @JsonProperty("explain_gen_process_flag")
        boolean explainGenProcessFlag,

        @JsonProperty("gen_suggested_question_flag")
        boolean genSuggestedQuestionFlag,

        @JsonProperty("answer_with_insights")
        boolean answerWithInsights,

        @JsonProperty("context_window")
        int contextWindow,

        @JsonProperty("query_rewrite")
        String queryRewrite,

        @JsonProperty("previous_intent")
        String previousIntent,

        @JsonProperty("entity_user_select")
        Map<String, Object> entityUserSelect,

        @JsonProperty("entity_retrieval")
        List<Object> entityRetrieval
) {
    // 預設值
    public Question {
        if (bedrockModelId == null) bedrockModelId = "anthropic.claude-3-sonnet-20240229-v1:0";
        if (sessionId == null) sessionId = "-1";
        if (userId == null) userId = "admin";
        if (username == null) username = "";
        if (queryRewrite == null) queryRewrite = "";
        if (previousIntent == null) previousIntent = "";
        if (entityUserSelect == null) entityUserSelect = Map.of();
        if (entityRetrieval == null) entityRetrieval = List.of();
        if (contextWindow < 0) contextWindow = 5;
    }
}
