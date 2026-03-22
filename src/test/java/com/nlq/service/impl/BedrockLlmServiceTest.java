package com.nlq.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlq.config.BedrockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;

/**
 * BedrockLlmService 單元測試 — Mock invokeModel，驗證 JSON 解析和 Prompt 整合
 */
@ExtendWith(MockitoExtension.class)
class BedrockLlmServiceTest {

    @Spy
    BedrockLlmService service = new BedrockLlmService(
            new BedrockProperties("test-model", "us-west-2", 4096, 0.01),
            new ObjectMapper(),
            new DefaultPromptService()  // Phase 5: 注入 PromptService
    );

    @Nested
    @DisplayName("getQueryIntent 意圖識別")
    class GetQueryIntentTests {

        @Test
        @DisplayName("正常 JSON 回傳 — 解析 intent + slot")
        void shouldParseIntentResponse() {
            doReturn("""
                    {"intent": "normal_search", "slot": [{"entity": "orders", "value": "total"}]}
                    """).when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            Map<String, Object> result = service.getQueryIntent("test-model", "show total orders", Map.of());

            assertEquals("normal_search", result.get("intent"));
            assertNotNull(result.get("slot"));
        }

        @Test
        @DisplayName("markdown code block 包裹的 JSON")
        void shouldParseMarkdownWrappedJson() {
            doReturn("""
                    ```json
                    {"intent": "knowledge_search", "slot": []}
                    ```
                    """).when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            Map<String, Object> result = service.getQueryIntent("test-model", "what is revenue", Map.of());

            assertEquals("knowledge_search", result.get("intent"));
        }
    }

    @Nested
    @DisplayName("getQueryRewrite 查詢改寫")
    class GetQueryRewriteTests {

        @Test
        @DisplayName("正常改寫回傳")
        void shouldParseRewriteResponse() {
            doReturn("""
                    {"intent": "normal", "query": "Show total sales for March 2026"}
                    """).when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            Map<String, Object> result = service.getQueryRewrite("test-model", "show last month sales",
                    Map.of(), List.of("show Q1 revenue"));

            assertEquals("normal", result.get("intent"));
            assertTrue(((String) result.get("query")).contains("March"));
        }

        @Test
        @DisplayName("ask_in_reply 意圖")
        void shouldHandleAskInReply() {
            doReturn("""
                    {"intent": "ask_in_reply", "query": "Which month do you mean?"}
                    """).when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            Map<String, Object> result = service.getQueryRewrite("test-model", "show sales",
                    Map.of(), List.of());

            assertEquals("ask_in_reply", result.get("intent"));
        }
    }

    @Nested
    @DisplayName("textToSql SQL 生成")
    class TextToSqlTests {

        @Test
        @DisplayName("回傳含 SQL 標籤的文字")
        void shouldReturnRawResponse() {
            String expected = "Based on the question:\n<sql>SELECT COUNT(*) FROM orders</sql>";
            doReturn(expected).when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            String result = service.textToSql("CREATE TABLE orders...", "amount=price*qty",
                    Map.of(), "how many orders", "test-model", List.of(), List.of(), "mysql");

            assertTrue(result.contains("<sql>"));
            assertTrue(result.contains("SELECT COUNT(*)"));
        }

        @Test
        @DisplayName("textToSql 使用 prompt_map 覆蓋的 system prompt")
        void shouldUseOverriddenPrompt() {
            doReturn("response").when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            Map<String, Object> customPromptMap = Map.of(
                    "text2sql", Map.of(
                            "system_prompt", "Custom SQL expert for {dialect}",
                            "user_prompt", "Schema: {sql_schema}\nQ: {question}\n{dialect_prompt}{examples}{ner_info}{sql_guidance}"
                    )
            );

            // 不拋異常 = 覆蓋成功
            service.textToSql("DDL", "hints", customPromptMap,
                    "query", "test-model", List.of(), List.of(), "mysql");
        }
    }

    @Nested
    @DisplayName("generateSuggestedQuestions 建議問題")
    class SuggestedQuestionsTests {

        @Test
        @DisplayName("解析 JSON 陣列")
        void shouldParseJsonArray() {
            doReturn("""
                    ["What is Q1 revenue?", "Top 5 products?", "Monthly trend?"]
                    """).when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            List<String> result = service.generateSuggestedQuestions(Map.of(), "show sales", "test-model");

            assertEquals(3, result.size());
            assertTrue(result.get(0).contains("Q1"));
        }
    }

    @Nested
    @DisplayName("dataVisualization 資料可視化")
    class DataVisualizationTests {

        @Test
        @DisplayName("解析圖表類型")
        void shouldParseChartType() {
            doReturn("""
                    {"showType": "chart", "chartType": "bar", "chartData": [{"x": "Jan", "y": 100}]}
                    """).when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            Map<String, Object> result = service.dataVisualization("test-model", "monthly sales",
                    List.of(Map.of("month", "Jan", "total", 100)), Map.of());

            assertEquals("chart", result.get("showType"));
            assertEquals("bar", result.get("chartType"));
        }
    }

    @Nested
    @DisplayName("dataAnalyse 數據分析")
    class DataAnalyseTests {

        @Test
        @DisplayName("agent 類型使用 AGENT_ANALYSE prompt")
        void agentType_usesAgentAnalysePrompt() {
            doReturn("Analysis result").when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            String result = service.dataAnalyse("test-model", Map.of(), "why dropped", "{}", "agent");
            assertEquals("Analysis result", result);
        }

        @Test
        @DisplayName("非 agent 類型使用 DATA_SUMMARY prompt")
        void otherType_usesDataSummaryPrompt() {
            doReturn("Summary result").when(service).invokeModel(anyString(), anyString(), anyString(), anyInt());

            String result = service.dataAnalyse("test-model", Map.of(), "total revenue", "{}", "summary");
            assertEquals("Summary result", result);
        }
    }

    @Nested
    @DisplayName("resolveModel 模型解析")
    class ModelResolutionTests {

        @Test
        @DisplayName("空 modelId 使用預設值")
        void shouldUseDefaultModel() {
            doReturn("{}").when(service).invokeModel(eq("test-model"), anyString(), anyString(), anyInt());

            service.knowledgeSearch("test", "", Map.of());
        }
    }
}
