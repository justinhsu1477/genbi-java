package com.nlq.service;

import java.util.List;
import java.util.Map;

/**
 * LLM 服務介面 - 封裝所有 AI 模型呼叫
 * LLM service interface — wraps all AI model invocations
 *
 * 對應 Python 的 utils/llm.py
 * Maps to Python's utils/llm.py
 */
public interface LlmService {

    /**
     * 意圖識別 + NER 槽位提取
     * Intent recognition + NER slot extraction
     *
     * @return {"intent": "normal_search|agent_search|knowledge_search|reject_search", "slot": [...]}
     */
    Map<String, Object> getQueryIntent(String modelId, String query, Map<String, Object> promptMap);

    /**
     * 查詢改寫 (根據歷史對話重寫問題)
     * Query rewrite based on conversation history
     *
     * @return {"intent": "normal|ask_in_reply", "query": "rewritten query"}
     */
    Map<String, Object> getQueryRewrite(String modelId, String query, Map<String, Object> promptMap, List<String> history);

    /**
     * 自然語言轉 SQL
     * Text to SQL generation
     *
     * @param tablesInfo DDL 資訊 table schema info
     * @param hints      提示 additional hints
     * @param query      查詢 user query
     * @param sqlExamples 範例 SQL examples from RAG
     * @param nerExamples NER 實體 entity examples
     * @param dialect    資料庫方言 db dialect (mysql/postgresql/etc)
     * @return 生成的 SQL 回應 generated SQL response text
     */
    String textToSql(String tablesInfo, String hints, Map<String, Object> promptMap,
                     String query, String modelId, List<Object> sqlExamples,
                     List<Object> nerExamples, String dialect);

    /**
     * 知識搜索 (LLM 直接回答)
     * Knowledge search — direct LLM answer
     */
    String knowledgeSearch(String query, String modelId, Map<String, Object> promptMap);

    /**
     * 數據分析
     * Data analysis — generate insights from query results
     */
    String dataAnalyse(String modelId, Map<String, Object> promptMap, String query, String dataJson, String type);

    /**
     * Agent 任務拆解
     * Agent CoT task split — break complex query into sub-tasks
     */
    Map<String, Object> getAgentCotTask(String modelId, Map<String, Object> promptMap,
                                         String query, String tablesInfo, List<Object> agentExamples);

    /**
     * 生成建議問題
     * Generate suggested follow-up questions
     */
    List<String> generateSuggestedQuestions(Map<String, Object> promptMap, String query, String modelId);

    /**
     * 數據可視化類型選擇
     * Select data visualization type (table/bar/line/pie)
     *
     * @return {"showType": "table|chart", "chartType": "bar|line|pie|-1", "chartData": [...]}
     */
    Map<String, Object> dataVisualization(String modelId, String query, List<Object> sqlData, Map<String, Object> promptMap);
}
