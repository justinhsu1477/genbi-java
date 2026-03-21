package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * 用戶提問請求 - 對應 Python 的 Question schema
 * User question request — maps to Python Question schema
 *
 * @param query              用戶的自然語言問題 the natural language question
 * @param bedrockModelId     LLM 模型 ID model identifier
 * @param profileName        資料庫 profile database profile name
 * @param sessionId          會話 ID session identifier
 * @param userId             用戶 ID user identifier
 * @param username           用戶名稱 username
 * @param useRagFlag         是否使用 RAG whether to use RAG retrieval
 * @param intentNerRecognitionFlag 是否做意圖識別 whether to do intent recognition
 * @param agentCotFlag       是否啟用 Agent 模式 whether to enable agent mode
 * @param explainGenProcessFlag 是否解釋生成過程 whether to explain SQL generation
 * @param genSuggestedQuestionFlag 是否生成建議問題 whether to generate follow-up questions
 * @param answerWithInsights 是否附帶數據分析 whether to include data insights
 * @param contextWindow      上下文窗口大小 conversation history size
 * @param queryRewrite       改寫後的查詢 rewritten query
 * @param previousIntent     前一次的意圖 previous intent
 * @param entityUserSelect   用戶選擇的實體 user selected entity
 * @param entityRetrieval    檢索到的實體 retrieved entities
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
    // 提供合理的預設值 provide sensible defaults
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
