package com.nlq.service.mock;

import com.nlq.service.RetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 向量檢索服務 Mock 實作 - 回傳空或假的檢索結果（僅 dev 環境）
 * Mock retrieval service — returns empty or fake retrieval results (dev profile only)
 *
 * 真實環境需要 OpenSearch + 向量索引
 * Real environment requires OpenSearch + vector indices
 *
 * 對應 Python: utils/opensearch.py + utils/text_search.py
 */
@Slf4j
@Service
@Profile("dev")
public class MockRetrievalService implements RetrievalService {

    /**
     * Mock 實體檢索 - 回傳空 (不觸發實體消歧)
     * Mock entity retrieval — return empty (won't trigger entity disambiguation)
     *
     * Python 原始: entity_retrieve_search() -> list of entity results
     * 每個結果格式: {"_score": 0.95, "_source": {"entity": "...", "entity_count": 1, ...}}
     */
    @Override
    public List<Map<String, Object>> entityRetrieveSearch(List<Object> entitySlots, String profileName) {
        log.info("[Mock Retrieval] entityRetrieveSearch: profileName={}, slots={}", profileName, entitySlots);
        // 回傳空 = 沒有實體匹配 = 直接往下走 QA_RETRIEVAL
        // Empty = no entity matches = proceed to QA_RETRIEVAL
        return List.of();
    }

    /**
     * Mock QA 檢索 - 回傳一個假的相似問答範例
     * Mock QA retrieval — return one fake similar Q&A example
     *
     * Python 原始: qa_retrieve_search() -> list of QA examples
     * 這些範例會作為 few-shot 提示給 LLM 生成更好的 SQL
     * These examples serve as few-shot prompts for better SQL generation
     */
    @Override
    public List<Object> qaRetrieveSearch(String query, String profileName) {
        log.info("[Mock Retrieval] qaRetrieveSearch: query={}, profileName={}", query, profileName);
        return List.of(
                Map.of(
                        "_score", 0.85,
                        "_source", Map.of(
                                "text", "Show me total sales",
                                "sql", "SELECT SUM(amount) FROM orders"
                        )
                )
        );
    }

    /**
     * Mock Agent 範例檢索
     * Mock agent example retrieval
     *
     * Python 原始: get_retrieve_opensearch() with agent_index
     */
    @Override
    public List<Object> agentRetrieveSearch(String query, String profileName) {
        log.info("[Mock Retrieval] agentRetrieveSearch: query={}", query);
        return List.of();
    }

    /**
     * Mock Agent 子任務 SQL 生成
     * Mock agent sub-task SQL generation
     *
     * Python 原始: agent_text_search() -> list of {query, sql, response}
     * 為每個子任務生成 SQL for each sub-task, generate SQL
     */
    @Override
    public List<Map<String, Object>> agentTextSearch(String query, String modelId,
                                                      Map<String, Object> databaseProfile,
                                                      List<Object> entitySlots, String profileName,
                                                      boolean useRag, Map<String, Object> taskSplit) {
        log.info("[Mock Retrieval] agentTextSearch: query={}, tasks={}", query, taskSplit);
        return List.of(
                Map.of(
                        "query", "Get total order amount",
                        "sql", "SELECT SUM(amount) AS total FROM orders",
                        "response", "Calculating total order amount"
                ),
                Map.of(
                        "query", "Get order count by product",
                        "sql", "SELECT product_id, COUNT(*) AS cnt FROM orders GROUP BY product_id",
                        "response", "Counting orders per product"
                )
        );
    }
}
