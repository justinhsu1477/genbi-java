package com.nlq.enums;

/**
 * 查詢狀態機的所有狀態
 */
public enum QueryState {
    INITIAL,              // 初始狀態
    INTENT_RECOGNITION,   // 意圖識別
    REJECT_INTENT,        // 拒絕回答
    KNOWLEDGE_SEARCH,     // 知識搜索（LLM 直接回答）
    ENTITY_RETRIEVAL,     // 實體檢索
    QA_RETRIEVAL,         // QA 範例檢索
    SQL_GENERATION,       // SQL 生成
    EXECUTE_QUERY,        // 執行 SQL
    ANALYZE_DATA,         // 數據分析
    AGENT_TASK,           // Agent 任務拆解
    AGENT_SEARCH,         // Agent SQL 生成
    AGENT_DATA_SUMMARY,   // Agent 數據匯總
    ASK_ENTITY_SELECT,    // 請求用戶選擇實體
    USER_SELECT_ENTITY,   // 用戶已選擇實體
    QUERY_REWRITE,        // 查詢改寫
    DATA_VISUALIZATION,   // 數據可視化
    COMPLETE,             // 完成
    ERROR                 // 錯誤
}
