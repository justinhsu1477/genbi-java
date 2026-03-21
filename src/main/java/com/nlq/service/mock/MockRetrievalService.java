package com.nlq.service.mock;

import com.nlq.service.RetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 向量檢索服務 Mock 實作 — 回傳空或假的檢索結果（僅 dev 環境，真實環境需要 OpenSearch）
 */
@Slf4j
@Service
@Profile("dev")
public class MockRetrievalService implements RetrievalService {

    /** Mock 實體檢索 — 回傳空（不觸發實體消歧） */
    @Override
    public List<Map<String, Object>> entityRetrieveSearch(List<Object> entitySlots, String profileName) {
        log.info("[Mock Retrieval] entityRetrieveSearch: profileName={}, slots={}", profileName, entitySlots);
        // 回傳空 = 沒有實體匹配 = 直接往下走 QA_RETRIEVAL
        return List.of();
    }

    /** Mock QA 檢索 — 回傳假的相似問答範例，作為 few-shot 提示 */
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

    /** Mock Agent 範例檢索 */
    @Override
    public List<Object> agentRetrieveSearch(String query, String profileName) {
        log.info("[Mock Retrieval] agentRetrieveSearch: query={}", query);
        return List.of();
    }

    /** Mock Agent 子任務 SQL 生成 — 為每個子任務生成假 SQL */
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
