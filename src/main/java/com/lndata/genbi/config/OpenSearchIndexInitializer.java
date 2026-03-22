package com.lndata.genbi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.common.settings.Settings;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * OpenSearch 索引初始化 — 應用啟動時自動建立缺少的索引
 *
 * 對應 Python: opensearch.py 中的 opensearch_index_init()
 * 三個索引: sql_index (QA), ner_index (Entity/NER), agent_index (Agent COT)
 */
@Slf4j
@Component
@Profile("!dev")
@RequiredArgsConstructor
public class OpenSearchIndexInitializer {

    private static final int EMBEDDING_DIMENSION = 1536;

    private final RestHighLevelClient openSearchClient;
    private final OpenSearchProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndices() {
        log.info("[OpenSearch] Checking and initializing indices...");

        List<String> indices = List.of(props.sqlIndex(), props.nerIndex(), props.agentIndex());

        for (String indexName : indices) {
            try {
                boolean exists = openSearchClient.indices()
                        .exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);

                if (!exists) {
                    createKnnIndex(indexName);
                    if (props.nerIndex().equals(indexName)) {
                        applyEntityMapping(indexName);
                    } else {
                        applyStandardMapping(indexName);
                    }
                    log.info("[OpenSearch] Index created: {}", indexName);
                } else {
                    log.info("[OpenSearch] Index already exists: {}", indexName);
                }
            } catch (IOException e) {
                log.error("[OpenSearch] Failed to initialize index {}: {}", indexName, e.getMessage(), e);
            }
        }

        log.info("[OpenSearch] Index initialization complete");
    }

    /** 建立啟用 KNN 的索引 */
    private void createKnnIndex(String indexName) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder()
                .put("index.knn", true)
                .put("index.knn.space_type", "cosinesimil")
        );
        openSearchClient.indices().create(request, RequestOptions.DEFAULT);
    }

    /** 標準 mapping (sql_index / agent_index) */
    private void applyStandardMapping(String indexName) throws IOException {
        PutMappingRequest request = new PutMappingRequest(indexName);
        request.source(Map.of("properties", Map.of(
                "vector_field", Map.of("type", "knn_vector", "dimension", EMBEDDING_DIMENSION),
                "text", Map.of("type", "keyword"),
                "profile", Map.of("type", "keyword")
        )));
        openSearchClient.indices().putMapping(request, RequestOptions.DEFAULT);
    }

    /** Entity 專用 mapping (ner_index)，多了 entity_type / entity_table_info */
    private void applyEntityMapping(String indexName) throws IOException {
        PutMappingRequest request = new PutMappingRequest(indexName);
        request.source(Map.of("properties", Map.of(
                "vector_field", Map.of("type", "knn_vector", "dimension", EMBEDDING_DIMENSION),
                "text", Map.of("type", "keyword"),
                "profile", Map.of("type", "keyword"),
                "entity_type", Map.of("type", "keyword"),
                "entity_count", Map.of("type", "integer"),
                "entity_table_info", Map.of(
                        "type", "nested",
                        "properties", Map.of(
                                "table_name", Map.of("type", "keyword"),
                                "column_name", Map.of("type", "keyword"),
                                "value", Map.of("type", "text")
                        )
                )
        )));
        openSearchClient.indices().putMapping(request, RequestOptions.DEFAULT);
    }
}
