package com.lndata.genbi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lndata.genbi.model.dto.*;
import com.lndata.genbi.exception.GlobalExceptionHandler;
import com.lndata.genbi.service.SampleManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SampleController 整合測試 — 使用 MockMvc 驗證 REST endpoint
 */
@ExtendWith(MockitoExtension.class)
class SampleControllerTest {

    @Mock private SampleManagementService sampleManagementService;
    @InjectMocks private SampleController sampleController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sampleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("SQL 範例端點")
    class SqlSampleTests {

        @Test
        @DisplayName("GET /api/v1/samples/sql → 回傳 SQL 範例列表")
        void getSqlSamples() throws Exception {
            when(sampleManagementService.getAllSqlSamples("demo"))
                    .thenReturn(List.of(
                            new SampleResponse("doc1", Map.of("text", "total sales", "sql", "SELECT SUM(amount) FROM orders"))
                    ));

            mockMvc.perform(get("/api/v1/samples/sql").param("profile_name", "demo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id").value("doc1"))
                    .andExpect(jsonPath("$.data[0].source.text").value("total sales"));
        }

        @Test
        @DisplayName("POST /api/v1/samples/sql → 新增 SQL 範例")
        void addSqlSample() throws Exception {
            SqlSampleRequest req = new SqlSampleRequest("demo", "total revenue", "SELECT SUM(revenue) FROM sales");

            mockMvc.perform(post("/api/v1/samples/sql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(sampleManagementService).addSqlSample(any(SqlSampleRequest.class));
        }

        @Test
        @DisplayName("POST /api/v1/samples/sql — 缺 question → 400")
        void addSqlSample_missingQuestion_returns400() throws Exception {
            String badBody = """
                    {"profile_name": "demo", "question": "", "sql": "SELECT 1"}
                    """;

            mockMvc.perform(post("/api/v1/samples/sql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("DELETE /api/v1/samples/sql/{docId} → 刪除成功")
        void deleteSqlSample() throws Exception {
            mockMvc.perform(delete("/api/v1/samples/sql/doc123")
                            .param("profile_name", "demo"))
                    .andExpect(status().isOk());

            verify(sampleManagementService).deleteSqlSample("demo", "doc123");
        }
    }

    @Nested
    @DisplayName("Entity 範例端點")
    class EntitySampleTests {

        @Test
        @DisplayName("GET /api/v1/samples/entities → 回傳 Entity 列表")
        void getEntitySamples() throws Exception {
            when(sampleManagementService.getAllEntitySamples("demo"))
                    .thenReturn(List.of(
                            new SampleResponse("e1", Map.of("entity", "台積電", "comment", "客戶名稱"))
                    ));

            mockMvc.perform(get("/api/v1/samples/entities").param("profile_name", "demo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].source.entity").value("台積電"));
        }

        @Test
        @DisplayName("POST /api/v1/samples/entities → 新增 Entity 範例")
        void addEntitySample() throws Exception {
            EntitySampleRequest req = new EntitySampleRequest("demo", "台積電", "客戶名稱", "metrics", List.of());

            mockMvc.perform(post("/api/v1/samples/entities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            verify(sampleManagementService).addEntitySample(any(EntitySampleRequest.class));
        }
    }

    @Nested
    @DisplayName("Agent COT 範例端點")
    class AgentCotSampleTests {

        @Test
        @DisplayName("GET /api/v1/samples/agents → 回傳 Agent COT 列表")
        void getAgentCotSamples() throws Exception {
            when(sampleManagementService.getAllAgentCotSamples("demo"))
                    .thenReturn(List.of(
                            new SampleResponse("a1", Map.of("query", "why revenue dropped", "comment", "step-by-step"))
                    ));

            mockMvc.perform(get("/api/v1/samples/agents").param("profile_name", "demo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].source.query").value("why revenue dropped"));
        }

        @Test
        @DisplayName("POST /api/v1/samples/agents → 新增 Agent COT 範例")
        void addAgentCotSample() throws Exception {
            AgentCotSampleRequest req = new AgentCotSampleRequest("demo", "compare Q1 Q2", "1. get Q1, 2. get Q2, 3. diff");

            mockMvc.perform(post("/api/v1/samples/agents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            verify(sampleManagementService).addAgentCotSample(any(AgentCotSampleRequest.class));
        }

        @Test
        @DisplayName("DELETE /api/v1/samples/agents/{docId} → 刪除成功")
        void deleteAgentCotSample() throws Exception {
            mockMvc.perform(delete("/api/v1/samples/agents/a123")
                            .param("profile_name", "demo"))
                    .andExpect(status().isOk());

            verify(sampleManagementService).deleteAgentCotSample("demo", "a123");
        }
    }
}
