package com.nlq.service.mock;

import com.nlq.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * LLM 服務的 Mock 實作 - 回傳寫死的假資料（僅 dev 環境）
 * Mock implementation of LlmService — returns hardcoded fake data (dev profile only)
 *
 * 用途：讓整個 WebSocket 流程能跑通，不需要真的呼叫 AWS Bedrock
 * Purpose: make the full WebSocket flow work without real AWS Bedrock calls
 *
 * 對應 Python: utils/llm.py
 */
@Slf4j
@Service
@Profile("dev")
public class MockLlmService implements LlmService {

    /**
     * Mock 意圖識別 - 根據查詢關鍵字決定意圖
     * Mock intent recognition — decide intent based on keywords
     *
     * Python 原始: get_query_intent() -> {"intent": "...", "slot": [...]}
     */
    @Override
    public Map<String, Object> getQueryIntent(String modelId, String query, Map<String, Object> promptMap) {
        log.info("[Mock LLM] getQueryIntent: query={}", query);

        // 根據關鍵字模擬不同意圖 simulate different intents by keywords
        String intent;
        if (query.toLowerCase().contains("what is") || query.toLowerCase().contains("explain")) {
            intent = "knowledge_search";
        } else if (query.toLowerCase().contains("compare") || query.toLowerCase().contains("and")) {
            intent = "agent_search";
        } else {
            intent = "normal_search";
        }

        return Map.of(
                "intent", intent,
                "slot", List.of(Map.of("entity", "orders", "value", "total"))
        );
    }

    /**
     * Mock 查詢改寫 - 簡單加上時間範圍
     * Mock query rewrite — simply add time range context
     *
     * Python 原始: get_query_rewrite() -> {"intent": "normal|ask_in_reply", "query": "..."}
     */
    @Override
    public Map<String, Object> getQueryRewrite(String modelId, String query,
                                                Map<String, Object> promptMap, List<String> history) {
        log.info("[Mock LLM] getQueryRewrite: query={}, historySize={}", query, history.size());

        // 模擬改寫：加上上下文 simulate rewrite with context
        String rewritten = history.isEmpty() ? query : query + " (based on recent conversation)";

        return Map.of(
                "intent", "normal",
                "query", rewritten
        );
    }

    /**
     * Mock Text-to-SQL - 根據查詢生成假 SQL
     * Mock text to SQL — generate fake SQL based on query keywords
     *
     * Python 原始: text_to_sql() -> (response_text, model_response)
     * 回傳格式含 <sql> 標籤 return format includes <sql> tags
     */
    @Override
    public String textToSql(String tablesInfo, String hints, Map<String, Object> promptMap,
                            String query, String modelId, List<Object> sqlExamples,
                            List<Object> nerExamples, String dialect) {
        log.info("[Mock LLM] textToSql: query={}, dialect={}", query, dialect);

        // 根據關鍵字生成不同的假 SQL generate different fake SQL by keywords
        String sql;
        if (query.toLowerCase().contains("total") || query.toLowerCase().contains("sum")) {
            sql = "SELECT SUM(amount) AS total_amount FROM orders";
        } else if (query.toLowerCase().contains("count")) {
            sql = "SELECT COUNT(*) AS order_count FROM orders";
        } else if (query.toLowerCase().contains("average") || query.toLowerCase().contains("avg")) {
            sql = "SELECT AVG(amount) AS avg_amount FROM orders";
        } else if (query.toLowerCase().contains("product")) {
            sql = "SELECT p.product_name, SUM(o.amount) AS total FROM orders o JOIN products p ON o.product_id = p.id GROUP BY p.product_name";
        } else {
            sql = "SELECT * FROM orders ORDER BY created_at DESC LIMIT 10";
        }

        // 模擬 LLM 回傳格式 (含解釋 + SQL 標籤)
        // Simulate LLM response format (with explanation + SQL tags)
        return "Based on the user's question, I need to query the orders table.\n<sql>" + sql + "</sql>";
    }

    /**
     * Mock 知識搜索 - 直接回答
     * Mock knowledge search — direct answer
     *
     * Python 原始: knowledge_search() -> (response_text, model_response)
     */
    @Override
    public String knowledgeSearch(String query, String modelId, Map<String, Object> promptMap) {
        log.info("[Mock LLM] knowledgeSearch: query={}", query);
        return "This is a mock knowledge response. In the real system, the LLM would answer: " + query;
    }

    /**
     * Mock 數據分析 - 產生假的 insights
     * Mock data analysis — generate fake insights
     *
     * Python 原始: data_analyse_tool() -> (response_text, model_response)
     */
    @Override
    public String dataAnalyse(String modelId, Map<String, Object> promptMap,
                              String query, String dataJson, String type) {
        log.info("[Mock LLM] dataAnalyse: query={}, type={}", query, type);
        return "Mock analysis: The query results show the requested data. "
                + "Key observations: data retrieved successfully from the database.";
    }

    /**
     * Mock Agent 任務拆解 - 拆成兩個子任務
     * Mock agent task split — split into 2 sub-tasks
     *
     * Python 原始: get_agent_cot_task() -> (task_dict, model_response)
     * 回傳格式: {"task_1": "...", "task_2": "..."}
     */
    @Override
    public Map<String, Object> getAgentCotTask(String modelId, Map<String, Object> promptMap,
                                                String query, String tablesInfo, List<Object> agentExamples) {
        log.info("[Mock LLM] getAgentCotTask: query={}", query);
        return Map.of(
                "task_1", "Get total order amount",
                "task_2", "Get order count by product"
        );
    }

    /**
     * Mock 生成建議問題
     * Mock suggested question generation
     *
     * Python 原始: generate_suggested_question()
     */
    @Override
    public List<String> generateSuggestedQuestions(Map<String, Object> promptMap, String query, String modelId) {
        log.info("[Mock LLM] generateSuggestedQuestions: query={}", query);
        return List.of(
                "What is the total revenue this month?",
                "Show me the top 5 products by sales",
                "How many orders were placed today?"
        );
    }

    /**
     * Mock 數據可視化類型選擇
     * Mock data visualization type selection
     *
     * Python 原始: data_visualization() -> (show_type, data, chart_type, chart_data, model_response)
     */
    @Override
    public Map<String, Object> dataVisualization(String modelId, String query,
                                                  List<Object> sqlData, Map<String, Object> promptMap) {
        log.info("[Mock LLM] dataVisualization: query={}, dataSize={}", query, sqlData.size());

        if (sqlData.size() <= 1) {
            return Map.of("showType", "table", "chartType", "-1", "chartData", List.of());
        }

        // 假設有多筆資料就用 bar chart simulate bar chart for multiple rows
        return Map.of(
                "showType", "bar",
                "chartType", "bar",
                "chartData", sqlData
        );
    }
}
