package com.nlq.service.mock;

import com.nlq.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * LLM 服務 Mock 實作 — 回傳寫死的假資料，讓 WebSocket 流程能跑通（僅 dev 環境）
 */
@Slf4j
@Service
@Profile("dev")
public class MockLlmService implements LlmService {

    /** Mock 意圖識別 — 根據查詢關鍵字決定意圖 */
    @Override
    public Map<String, Object> getQueryIntent(String modelId, String query, Map<String, Object> promptMap) {
        log.info("[Mock LLM] getQueryIntent: query={}", query);

        // 根據關鍵字模擬不同意圖
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

    /** Mock 查詢改寫 — 簡單加上上下文 */
    @Override
    public Map<String, Object> getQueryRewrite(String modelId, String query,
                                                Map<String, Object> promptMap, List<String> history) {
        log.info("[Mock LLM] getQueryRewrite: query={}, historySize={}", query, history.size());

        // 模擬改寫：加上上下文
        String rewritten = history.isEmpty() ? query : query + " (based on recent conversation)";

        return Map.of(
                "intent", "normal",
                "query", rewritten
        );
    }

    /** Mock Text-to-SQL — 根據查詢關鍵字生成假 SQL，回傳格式含 &lt;sql&gt; 標籤 */
    @Override
    public String textToSql(String tablesInfo, String hints, Map<String, Object> promptMap,
                            String query, String modelId, List<Object> sqlExamples,
                            List<Object> nerExamples, String dialect) {
        log.info("[Mock LLM] textToSql: query={}, dialect={}", query, dialect);

        // 根據關鍵字生成不同的假 SQL
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

        // 模擬 LLM 回傳格式（含解釋 + SQL 標籤）
        return "Based on the user's question, I need to query the orders table.\n<sql>" + sql + "</sql>";
    }

    /** Mock SQL 自動修正 — 模擬修正後的 SQL */
    @Override
    public String textToSqlWithCorrection(String tablesInfo, String hints, Map<String, Object> promptMap,
                                           String query, String modelId, List<Object> sqlExamples,
                                           List<Object> nerExamples, String dialect,
                                           String originalSql, String errorInfo) {
        log.info("[Mock LLM] textToSqlWithCorrection: query={}, error={}", query, errorInfo);
        return "After reviewing the error, here is the corrected SQL.\n<sql>SELECT COUNT(*) AS total FROM orders</sql>";
    }

    /** Mock 知識搜索 — 直接回答 */
    @Override
    public String knowledgeSearch(String query, String modelId, Map<String, Object> promptMap) {
        log.info("[Mock LLM] knowledgeSearch: query={}", query);
        return "This is a mock knowledge response. In the real system, the LLM would answer: " + query;
    }

    /** Mock 數據分析 — 產生假的 insights */
    @Override
    public String dataAnalyse(String modelId, Map<String, Object> promptMap,
                              String query, String dataJson, String type) {
        log.info("[Mock LLM] dataAnalyse: query={}, type={}", query, type);
        return "Mock analysis: The query results show the requested data. "
                + "Key observations: data retrieved successfully from the database.";
    }

    /** Mock Agent 任務拆解 — 拆成兩個子任務 */
    @Override
    public Map<String, Object> getAgentCotTask(String modelId, Map<String, Object> promptMap,
                                                String query, String tablesInfo, List<Object> agentExamples) {
        log.info("[Mock LLM] getAgentCotTask: query={}", query);
        return Map.of(
                "task_1", "Get total order amount",
                "task_2", "Get order count by product"
        );
    }

    /** Mock 生成建議問題 */
    @Override
    public List<String> generateSuggestedQuestions(Map<String, Object> promptMap, String query, String modelId) {
        log.info("[Mock LLM] generateSuggestedQuestions: query={}", query);
        return List.of(
                "What is the total revenue this month?",
                "Show me the top 5 products by sales",
                "How many orders were placed today?"
        );
    }

    /** Mock 數據可視化類型選擇 */
    @Override
    public Map<String, Object> dataVisualization(String modelId, String query,
                                                  List<Object> sqlData, Map<String, Object> promptMap) {
        log.info("[Mock LLM] dataVisualization: query={}, dataSize={}", query, sqlData.size());

        if (sqlData.size() <= 1) {
            return Map.of("showType", "table", "chartType", "-1", "chartData", List.of());
        }

        // 假設有多筆資料就用 bar chart
        return Map.of(
                "showType", "bar",
                "chartType", "bar",
                "chartData", sqlData
        );
    }
}
