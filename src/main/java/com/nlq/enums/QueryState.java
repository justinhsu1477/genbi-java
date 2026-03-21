package com.nlq.enums;

/**
 * 查詢狀態機的所有狀態
 * All states in the query state machine
 */
public enum QueryState {
    INITIAL,              // 初始狀態 initial state
    INTENT_RECOGNITION,   // 意圖識別 recognize user intent
    REJECT_INTENT,        // 拒絕回答 refuse to answer
    KNOWLEDGE_SEARCH,     // 知識搜索 direct LLM answer (no SQL)
    ENTITY_RETRIEVAL,     // 實體檢索 vector search for entities
    QA_RETRIEVAL,         // QA 範例檢索 vector search for similar Q&A
    SQL_GENERATION,       // SQL 生成 generate SQL from text
    EXECUTE_QUERY,        // 執行 SQL execute SQL against database
    ANALYZE_DATA,         // 數據分析 generate insights from results
    AGENT_TASK,           // Agent 任務拆解 break query into subtasks
    AGENT_SEARCH,         // Agent SQL 生成 generate SQL for each subtask
    AGENT_DATA_SUMMARY,   // Agent 數據匯總 summarize agent results
    ASK_ENTITY_SELECT,    // 請求用戶選擇實體 ask user to disambiguate
    USER_SELECT_ENTITY,   // 用戶已選擇實體 process user's entity selection
    QUERY_REWRITE,        // 查詢改寫 rewrite query with context
    DATA_VISUALIZATION,   // 數據可視化 select chart type
    COMPLETE,             // 完成 done
    ERROR                 // 錯誤 error
}
