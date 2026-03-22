package com.nlq.service.impl;

import com.nlq.config.OpenSearchProperties;
import com.nlq.dto.*;
import com.nlq.exception.BusinessException;
import com.nlq.service.EmbeddingService;
import com.nlq.service.SampleManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * OpenSearch 範例管理服務 — 真實實作
 *
 * 對應 Python: VectorStore + OpenSearchDao
 * 管理三個索引: sql_index (QA), ner_index (Entity), agent_index (Agent COT)
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class OpenSearchSampleManagementService implements SampleManagementService {

    private final RestHighLevelClient openSearchClient;
    private final EmbeddingService embeddingService;
    private final OpenSearchProperties props;

    // =====================================================
    // SQL 範例 CRUD
    // =====================================================

    @Override
    public List<SampleResponse> getAllSqlSamples(String profileName) {
        log.info("[Sample] getAllSqlSamples: profileName={}", profileName);
        return retrieveAllByProfile(props.sqlIndex(), profileName, List.of("text", "sql"));
    }

    @Override
    public void addSqlSample(SqlSampleRequest request) {
        log.info("[Sample] addSqlSample: profile={}, question={}", request.profileName(), request.question());

        List<Float> embedding = embeddingService.createEmbedding(request.question());

        // 檢查並刪除重複
        deleteDuplicateIfExists(props.sqlIndex(), request.profileName(), embedding);

        // 新增文件
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("text", request.question());
        doc.put("sql", request.sql());
        doc.put("profile", request.profileName());
        doc.put("vector_field", embedding);

        indexDocument(props.sqlIndex(), doc);
    }

    @Override
    public void deleteSqlSample(String profileName, String docId) {
        log.info("[Sample] deleteSqlSample: profile={}, docId={}", profileName, docId);
        deleteDocument(props.sqlIndex(), docId);
    }

    // =====================================================
    // Entity 範例 CRUD
    // =====================================================

    @Override
    public List<SampleResponse> getAllEntitySamples(String profileName) {
        log.info("[Sample] getAllEntitySamples: profileName={}", profileName);
        return retrieveAllByProfile(props.nerIndex(), profileName,
                List.of("entity", "comment", "entity_type", "entity_count", "entity_table_info"));
    }

    @Override
    public void addEntitySample(EntitySampleRequest request) {
        log.info("[Sample] addEntitySample: profile={}, entity={}", request.profileName(), request.entity());

        List<Float> embedding = embeddingService.createEmbedding(request.entity());

        // 組合 comment（dimension 類型自動拼接說明）
        String comment = buildEntityComment(request);
        int entityCount = request.entityTableInfo().size();

        // 新增文件
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("entity", request.entity());
        doc.put("comment", comment);
        doc.put("profile", request.profileName());
        doc.put("vector_field", embedding);
        doc.put("entity_type", request.entityType());
        doc.put("entity_count", entityCount);
        doc.put("entity_table_info", request.entityTableInfo());

        indexDocument(props.nerIndex(), doc);
    }

    @Override
    public void deleteEntitySample(String profileName, String docId) {
        log.info("[Sample] deleteEntitySample: profile={}, docId={}", profileName, docId);
        deleteDocument(props.nerIndex(), docId);
    }

    // =====================================================
    // Agent COT 範例 CRUD
    // =====================================================

    @Override
    public List<SampleResponse> getAllAgentCotSamples(String profileName) {
        log.info("[Sample] getAllAgentCotSamples: profileName={}", profileName);
        return retrieveAllByProfile(props.agentIndex(), profileName, List.of("query", "comment"));
    }

    @Override
    public void addAgentCotSample(AgentCotSampleRequest request) {
        log.info("[Sample] addAgentCotSample: profile={}, query={}", request.profileName(), request.query());

        List<Float> embedding = embeddingService.createEmbedding(request.query());

        // 檢查並刪除重複
        deleteDuplicateIfExists(props.agentIndex(), request.profileName(), embedding);

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("query", request.query());
        doc.put("comment", request.comment());
        doc.put("profile", request.profileName());
        doc.put("vector_field", embedding);

        indexDocument(props.agentIndex(), doc);
    }

    @Override
    public void deleteAgentCotSample(String profileName, String docId) {
        log.info("[Sample] deleteAgentCotSample: profile={}, docId={}", profileName, docId);
        deleteDocument(props.agentIndex(), docId);
    }

    // =====================================================
    // 內部方法 — OpenSearch 操作
    // =====================================================

    /**
     * 查詢指定 profile 的所有文件
     * 對應 Python: OpenSearchDao.retrieve_samples() / retrieve_entity_samples()
     */
    private List<SampleResponse> retrieveAllByProfile(String indexName, String profileName, List<String> includes) {
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(QueryBuilders.boolQuery()
                            .filter(QueryBuilders.matchPhraseQuery("profile", profileName)))
                    .fetchSource(includes.toArray(String[]::new), null)
                    .size(5000);

            SearchRequest request = new SearchRequest(indexName);
            request.source(sourceBuilder);

            SearchResponse response = openSearchClient.search(request, RequestOptions.DEFAULT);

            List<SampleResponse> results = new ArrayList<>();
            for (SearchHit hit : response.getHits().getHits()) {
                results.add(new SampleResponse(hit.getId(), hit.getSourceAsMap()));
            }
            return results;

        } catch (IOException e) {
            log.error("[Sample] retrieveAllByProfile failed: index={}, error={}", indexName, e.getMessage(), e);
            throw BusinessException.serverError("Failed to retrieve samples: " + e.getMessage());
        }
    }

    /** 新增單一文件到 OpenSearch */
    private void indexDocument(String indexName, Map<String, Object> document) {
        try {
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.add(new IndexRequest(indexName).source(document));

            BulkResponse response = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);

            if (response.hasFailures()) {
                log.error("[Sample] indexDocument failed: {}", response.buildFailureMessage());
                throw BusinessException.serverError("Failed to index document: " + response.buildFailureMessage());
            }
            log.info("[Sample] Document indexed: index={}", indexName);

        } catch (IOException e) {
            log.error("[Sample] indexDocument error: index={}, error={}", indexName, e.getMessage(), e);
            throw BusinessException.serverError("Failed to index document: " + e.getMessage());
        }
    }

    /** 刪除指定文件 */
    private void deleteDocument(String indexName, String docId) {
        try {
            openSearchClient.delete(new DeleteRequest(indexName, docId), RequestOptions.DEFAULT);
            log.info("[Sample] Document deleted: index={}, docId={}", indexName, docId);
        } catch (IOException e) {
            log.error("[Sample] deleteDocument error: index={}, docId={}, error={}", indexName, docId, e.getMessage(), e);
            throw BusinessException.serverError("Failed to delete document: " + e.getMessage());
        }
    }

    /**
     * 檢查重複文件（完全相同 = score 1.0），存在則刪除
     * 對應 Python: VectorStore.search_same_query()
     */
    private void deleteDuplicateIfExists(String indexName, String profileName, List<Float> embedding) {
        try {
            float[] embeddingArray = toFloatArray(embedding);

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(QueryBuilders.boolQuery()
                            .filter(QueryBuilders.matchPhraseQuery("profile", profileName))
                            .must(QueryBuilders.scriptScoreQuery(
                                    QueryBuilders.matchAllQuery(),
                                    new org.opensearch.script.Script(
                                            org.opensearch.script.Script.DEFAULT_SCRIPT_TYPE,
                                            "knn",
                                            "knn_score",
                                            Map.of("field", "vector_field", "query_value", embeddingArray, "space_type", "cosinesimil")
                                    )
                            )))
                    .size(1);

            SearchRequest request = new SearchRequest(indexName);
            request.source(sourceBuilder);

            SearchResponse response = openSearchClient.search(request, RequestOptions.DEFAULT);

            if (response.getHits().getHits().length > 0) {
                SearchHit hit = response.getHits().getHits()[0];
                if (hit.getScore() >= 1.0f) {
                    log.info("[Sample] Found duplicate (score=1.0), deleting: id={}", hit.getId());
                    deleteDocument(indexName, hit.getId());
                }
            }

        } catch (IOException e) {
            log.warn("[Sample] Duplicate check failed (non-critical): {}", e.getMessage());
        }
    }

    /**
     * 組合 Entity 的 comment — dimension 類型自動拼接表欄位說明
     * 對應 Python: OpenSearchDao.add_entity_sample() 中的 dimension comment 邏輯
     */
    private String buildEntityComment(EntitySampleRequest request) {
        if (!"dimension".equals(request.entityType()) || request.entityTableInfo().isEmpty()) {
            return request.comment();
        }

        List<String> parts = new ArrayList<>();
        for (Map<String, String> info : request.entityTableInfo()) {
            String fmt = "%s is located in table %s, column %s, the dimension value is %s.".formatted(
                    request.entity(),
                    info.getOrDefault("table_name", ""),
                    info.getOrDefault("column_name", ""),
                    info.getOrDefault("value", "")
            );
            parts.add(fmt);
        }
        return String.join(";", parts);
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
