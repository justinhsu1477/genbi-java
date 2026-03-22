package com.lndata.genbi.service;

import java.util.List;
import java.util.Map;

/**
 * LLM 服務介面 — 封裝所有 AI 模型呼叫
 */
public interface LlmService {

    /**
     * 意圖識別 + NER 槽位提取
     *
     * @return {"intent": "normal_search|agent_search|knowledge_search|reject_search", "slot": [...]}
     */
    Map<String, Object> getQueryIntent(String modelId, String query, Map<String, Object> promptMap);

    /**
     * 查詢改寫（根據歷史對話重寫問題）
     *
     * @return {"intent": "normal|ask_in_reply", "query": "rewritten query"}
     */
    Map<String, Object> getQueryRewrite(String modelId, String query, Map<String, Object> promptMap, List<String> history);

    /**
     * 自然語言轉 SQL
     *
     * @param tablesInfo  DDL 資訊
     * @param hints       補充提示
     * @param query       用戶查詢
     * @param sqlExamples RAG 檢索到的 SQL 範例
     * @param nerExamples NER 實體範例
     * @param dialect     資料庫方言 (mysql/postgresql/etc)
     * @return 生成的 SQL 回應文字
     */
    String textToSql(String tablesInfo, String hints, Map<String, Object> promptMap,
                     String query, String modelId, List<Object> sqlExamples,
                     List<Object> nerExamples, String dialect);

    /** 知識搜索（LLM 直接回答） */
    String knowledgeSearch(String query, String modelId, Map<String, Object> promptMap);

    /**
     * SQL 自動修正 — 將錯誤的 SQL 和錯誤訊息餵回 LLM 重新生成
     *
     * @param originalSql 原本生成的 SQL
     * @param errorInfo   執行錯誤訊息
     * @return 修正後的 SQL 回應文字
     */
    String textToSqlWithCorrection(String tablesInfo, String hints, Map<String, Object> promptMap,
                                    String query, String modelId, List<Object> sqlExamples,
                                    List<Object> nerExamples, String dialect,
                                    String originalSql, String errorInfo);

    /** 數據分析 — 從查詢結果產生 insights */
    String dataAnalyse(String modelId, Map<String, Object> promptMap, String query, String dataJson, String type);

    /** Agent 任務拆解 — 將複雜查詢拆成子任務 */
    Map<String, Object> getAgentCotTask(String modelId, Map<String, Object> promptMap,
                                         String query, String tablesInfo, List<Object> agentExamples);

    /** 生成建議問題 */
    List<String> generateSuggestedQuestions(Map<String, Object> promptMap, String query, String modelId);

    /**
     * 數據可視化類型選擇
     *
     * @return {"showType": "table|chart", "chartType": "bar|line|pie|-1", "chartData": [...]}
     */
    Map<String, Object> dataVisualization(String modelId, String query, List<Object> sqlData, Map<String, Object> promptMap);
}
