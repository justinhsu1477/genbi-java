package com.lndata.genbi.service.impl;

import com.lndata.genbi.config.OpenSearchProperties;
import com.lndata.genbi.service.EmbeddingService;
import com.lndata.genbi.service.LlmService;
import com.lndata.genbi.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * OpenSearch RAG 檢索服務 — 真實向量搜尋實作 (qas/prod 環境)
 *
 * 對應 Python: vector_store.py + opensearch.py
 * 使用 KNN 搜尋在 OpenSearch 中找語意最相似的範例
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class OpenSearchRetrievalService implements RetrievalService {

    private final RestHighLevelClient openSearchClient;
    private final EmbeddingService embeddingService;
    private final LlmService llmService;
    private final OpenSearchProperties openSearchProperties;

    /** 預設 KNN top-k */
    private static final int DEFAULT_TOP_K = 5;
    /** 相似度分數門檻 */
    private static final double SCORE_THRESHOLD = 0.7;

    // =====================================================
    // RetrievalService 介面實作
    // =====================================================

    /**
     * 實體檢索 — 用 NER 槽位的 entity 向量搜尋 ner_index
     * 對應 Python: get_retrieve_opensearch(search_type="ner")
     */
    @Override
    public List<Map<String, Object>> entityRetrieveSearch(List<Object> entitySlots, String profileName) {
        log.info("[OpenSearch] entityRetrieveSearch: profileName={}, slots={}", profileName, entitySlots.size());

        if (entitySlots.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> allResults = new ArrayList<>();

        for (Object slot : entitySlots) {
            @SuppressWarnings("unchecked")
            Map<String, Object> slotMap = (Map<String, Object>) slot;
            String entity = (String) slotMap.getOrDefault("entity", slotMap.getOrDefault("value", ""));
            if (entity.isBlank()) continue;

            List<Map<String, Object>> results = knnSearch(
                    openSearchProperties.nerIndex(), entity, profileName, DEFAULT_TOP_K);
            allResults.addAll(results);
        }

        log.info("[OpenSearch] entityRetrieveSearch: found {} results", allResults.size());
        return allResults;
    }

    /**
     * QA 範例檢索 — 用查詢文字向量搜尋 sql_index，找相似問答對作為 few-shot
     * 對應 Python: get_retrieve_opensearch(search_type="query")
     */
    @Override
    public List<Object> qaRetrieveSearch(String query, String profileName) {
        log.info("[OpenSearch] qaRetrieveSearch: query={}, profileName={}", truncate(query), profileName);

        List<Map<String, Object>> results = knnSearch(
                openSearchProperties.sqlIndex(), query, profileName, DEFAULT_TOP_K);

        log.info("[OpenSearch] qaRetrieveSearch: found {} results", results.size());
        return new ArrayList<>(results);
    }

    /**
     * Agent 範例檢索 — 用查詢文字向量搜尋 agent_index
     * 對應 Python: get_retrieve_opensearch(search_type="agent")
     */
    @Override
    public List<Object> agentRetrieveSearch(String query, String profileName) {
        log.info("[OpenSearch] agentRetrieveSearch: query={}, profileName={}", truncate(query), profileName);

        List<Map<String, Object>> results = knnSearch(
                openSearchProperties.agentIndex(), query, profileName, DEFAULT_TOP_K);

        log.info("[OpenSearch] agentRetrieveSearch: found {} results", results.size());
        return new ArrayList<>(results);
    }

    /**
     * Agent 子任務 SQL 生成 — 為每個子任務做 QA 檢索 + LLM 生成 SQL
     * 對應 Python: state_machine 中 agent_sql_generation 的邏輯
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> agentTextSearch(String query, String modelId,
                                                      Map<String, Object> databaseProfile,
                                                      List<Object> entitySlots, String profileName,
                                                      boolean useRag, Map<String, Object> taskSplit) {
        log.info("[OpenSearch] agentTextSearch: query={}, tasks={}", truncate(query), taskSplit.size());

        String tablesInfo = (String) databaseProfile.getOrDefault("tables_info", "");
        String hints = (String) databaseProfile.getOrDefault("hints", "");
        String dialect = (String) databaseProfile.getOrDefault("db_type", "mysql");
        Map<String, Object> promptMap = (Map<String, Object>) databaseProfile.getOrDefault("prompt_map", Map.of());

        List<Map<String, Object>> taskResults = new ArrayList<>();

        for (Map.Entry<String, Object> entry : taskSplit.entrySet()) {
            String taskQuery = String.valueOf(entry.getValue());

            // 為子任務做 QA 檢索
            List<Object> subQaExamples = useRag
                    ? qaRetrieveSearch(taskQuery, profileName)
                    : List.of();

            // LLM 生成 SQL
            try {
                String sqlResponse = llmService.textToSql(
                        tablesInfo, hints, promptMap, taskQuery, modelId,
                        subQaExamples, List.of(), dialect);

                taskResults.add(Map.of(
                        "query", taskQuery,
                        "sql", extractSql(sqlResponse),
                        "response", sqlResponse
                ));
            } catch (Exception e) {
                log.warn("[OpenSearch] agentTextSearch failed for task {}: {}", entry.getKey(), e.getMessage());
                taskResults.add(Map.of(
                        "query", taskQuery,
                        "sql", "",
                        "response", "Error: " + e.getMessage()
                ));
            }
        }

        return taskResults;
    }

    // =====================================================
    // 核心 KNN 搜尋
    // =====================================================

    /**
     * KNN 向量搜尋 — 核心方法
     * 對應 Python: retrieve_results_from_opensearch()
     *
     * @param indexName   OpenSearch 索引名稱
     * @param queryText   查詢文字（會先向量化）
     * @param profileName 過濾用的 profile 名稱
     * @param topK        回傳前 K 筆最相似結果
     * @return 搜尋結果列表，每筆包含 _id, _score, _source
     */
    List<Map<String, Object>> knnSearch(String indexName, String queryText,
                                         String profileName, int topK) {
        try {
            // 1. 將查詢文字向量化
            List<Float> queryEmbedding = embeddingService.createEmbedding(queryText);

            // 2. 建構 KNN 搜尋請求
            float[] embeddingArray = toFloatArray(queryEmbedding);

            // KNN query + profile filter
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.matchPhraseQuery("profile", profileName))
                    .must(QueryBuilders.scriptScoreQuery(
                            QueryBuilders.matchAllQuery(),
                            new org.opensearch.script.Script(
                                    org.opensearch.script.Script.DEFAULT_SCRIPT_TYPE,
                                    "knn",
                                    "knn_score",
                                    Map.of("field", "vector_field", "query_value", embeddingArray, "space_type", "cosinesimil")
                            )
                    ));

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(boolQuery)
                    .size(topK);

            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(sourceBuilder);

            // 3. 執行搜尋
            SearchResponse response = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);

            // 4. 轉換結果，過濾低分
            List<Map<String, Object>> results = new ArrayList<>();
            for (SearchHit hit : response.getHits().getHits()) {
                if (hit.getScore() < SCORE_THRESHOLD) continue;

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("_id", hit.getId());
                result.put("_score", hit.getScore());
                result.put("_source", hit.getSourceAsMap());
                results.add(result);
            }

            return results;

        } catch (IOException e) {
            log.error("[OpenSearch] knnSearch failed: index={}, error={}", indexName, e.getMessage(), e);
            return List.of();
        }
    }

    // =====================================================
    // 工具方法
    // =====================================================

    /** 從 LLM 回應中提取 SQL */
    private String extractSql(String response) {
        if (response.contains("<sql>") && response.contains("</sql>")) {
            int start = response.indexOf("<sql>") + 5;
            int end = response.indexOf("</sql>");
            return response.substring(start, end).strip();
        }
        return response.strip();
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private String truncate(String s) {
        return s != null && s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }
}
