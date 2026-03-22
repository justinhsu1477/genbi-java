package com.lndata.genbi.service.impl;

import com.lndata.genbi.config.OpenSearchProperties;
import com.lndata.genbi.service.EmbeddingService;
import com.lndata.genbi.service.LlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OpenSearchRetrievalService 單元測試
 */
@ExtendWith(MockitoExtension.class)
class OpenSearchRetrievalServiceTest {

    @Mock private org.opensearch.client.RestHighLevelClient openSearchClient;
    @Mock private EmbeddingService embeddingService;
    @Mock private LlmService llmService;

    private OpenSearchProperties props;
    private OpenSearchRetrievalService service;

    @BeforeEach
    void setUp() {
        props = new OpenSearchProperties("localhost", 9200, "admin", "admin",
                "http", "uba", "uba_ner", "uba_agent", "genbi_query_logging");
        service = spy(new OpenSearchRetrievalService(openSearchClient, embeddingService, llmService, props));
    }

    @Nested
    @DisplayName("entityRetrieveSearch")
    class EntityRetrieveSearchTest {

        @Test
        @DisplayName("空 slots → 回傳空列表")
        void emptySlots_returnsEmpty() {
            List<Map<String, Object>> result = service.entityRetrieveSearch(List.of(), "test_profile");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("有 slots → 呼叫 knnSearch")
        void withSlots_callsKnnSearch() {
            doReturn(List.of(Map.of("_id", "1", "_score", 0.95, "_source", Map.of("entity", "台積電"))))
                    .when(service).knnSearch(eq("uba_ner"), eq("台積電"), eq("test_profile"), anyInt());

            List<Object> slots = List.of(Map.of("entity", "台積電"));
            List<Map<String, Object>> result = service.entityRetrieveSearch(slots, "test_profile");

            assertEquals(1, result.size());
            verify(service).knnSearch(eq("uba_ner"), eq("台積電"), eq("test_profile"), anyInt());
        }
    }

    @Nested
    @DisplayName("qaRetrieveSearch")
    class QaRetrieveSearchTest {

        @Test
        @DisplayName("正常查詢 → 呼叫 knnSearch 搜尋 sql_index")
        void normalQuery_searchesSqlIndex() {
            doReturn(List.of(
                    Map.of("_id", "1", "_score", 0.88,
                            "_source", Map.of("text", "monthly revenue", "sql", "SELECT ..."))
            )).when(service).knnSearch(eq("uba"), anyString(), eq("profile_a"), anyInt());

            List<Object> result = service.qaRetrieveSearch("月營收", "profile_a");

            assertEquals(1, result.size());
            verify(service).knnSearch(eq("uba"), eq("月營收"), eq("profile_a"), anyInt());
        }
    }

    @Nested
    @DisplayName("agentRetrieveSearch")
    class AgentRetrieveSearchTest {

        @Test
        @DisplayName("正常查詢 → 呼叫 knnSearch 搜尋 agent_index")
        void normalQuery_searchesAgentIndex() {
            doReturn(List.of()).when(service).knnSearch(eq("uba_agent"), anyString(), anyString(), anyInt());

            List<Object> result = service.agentRetrieveSearch("why revenue dropped", "p1");

            assertTrue(result.isEmpty());
            verify(service).knnSearch(eq("uba_agent"), anyString(), eq("p1"), anyInt());
        }
    }

    @Nested
    @DisplayName("agentTextSearch")
    class AgentTextSearchTest {

        @Test
        @DisplayName("為每個子任務生成 SQL")
        void generatesPerTask() {
            doReturn(List.of()).when(service).knnSearch(anyString(), anyString(), anyString(), anyInt());
            when(llmService.textToSql(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn("<sql>SELECT 1</sql>");

            Map<String, Object> tasks = Map.of("task_1", "Get revenue", "task_2", "Get cost");
            Map<String, Object> dbProfile = Map.of(
                    "tables_info", "DDL here",
                    "hints", "",
                    "db_type", "mysql",
                    "prompt_map", Map.of()
            );

            List<Map<String, Object>> result = service.agentTextSearch(
                    "compare revenue and cost", "model-1", dbProfile,
                    List.of(), "p1", true, tasks);

            assertEquals(2, result.size());
            assertEquals("SELECT 1", result.get(0).get("sql"));
        }

        @Test
        @DisplayName("LLM 失敗時不中斷，返回 error")
        void llmFailure_doesNotThrow() {
            when(llmService.textToSql(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("LLM timeout"));

            Map<String, Object> tasks = Map.of("task_1", "Get data");
            Map<String, Object> dbProfile = Map.of(
                    "tables_info", "", "hints", "", "db_type", "mysql", "prompt_map", Map.of());

            List<Map<String, Object>> result = service.agentTextSearch(
                    "query", "m1", dbProfile, List.of(), "p1", false, tasks);

            assertEquals(1, result.size());
            assertEquals("", result.get(0).get("sql"));
        }
    }
}
