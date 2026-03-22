package com.nlq.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlq.config.BedrockProperties;
import com.nlq.enums.PromptType;
import com.nlq.enums.SqlDialect;
import com.nlq.service.LlmService;
import com.nlq.service.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Bedrock LLM 服務 — 真實呼叫 Claude (qas/prod 環境)
 *
 * Phase 5 重構: 使用 PromptService 統一管理 prompt 模板，
 * 取代原本硬編碼的 fallback prompt。
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class BedrockLlmService implements LlmService {

    private final BedrockProperties bedrockProperties;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    private volatile BedrockRuntimeClient client;

    // --- Agent COT 預設範例 (Python: AGENT_COT_EXAMPLE) ---
    private static final String DEFAULT_AGENT_COT_EXAMPLE = """
            question: Why did the order sales volume of commodities decline in June?
            tables:\s
            interactions, The data on users' interactions with products
            items, The product information table
            users, The user information table

            The analysis approach:
            1. Analyze the total sales volume and sales revenue of the top 10 products.
            2. Analyze the purchase situation of the top 10 products by different genders.
            3. Analyze the most popular product category with the highest purchase rate.

            answer:
            ```json
            {
                "task_1":"Analyze the total sales volume and sales revenue of the top 10 products.",
                "task_2":"Analyze the purchase situation of the top 10 products by different genders",
                "task_3":"Analyze the most popular product category with the highest purchase rate."
            }
            ```
            """;

    // --- 公開介面實作 ---

    @Override
    public Map<String, Object> getQueryIntent(String modelId, String query, Map<String, Object> promptMap) {
        log.info("[Bedrock] getQueryIntent: model={}, query={}", resolveModel(modelId), truncate(query));

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.INTENT, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.INTENT, modelId,
                Map.of("question", query));

        String response = invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public Map<String, Object> getQueryRewrite(String modelId, String query,
                                                 Map<String, Object> promptMap, List<String> history) {
        log.info("[Bedrock] getQueryRewrite: model={}, query={}, historySize={}", resolveModel(modelId), truncate(query), history.size());

        String chatHistory = String.join("\n", history);
        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.QUERY_REWRITE, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.QUERY_REWRITE, modelId,
                Map.of("chat_history", chatHistory, "question", query));

        String response = invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public String textToSql(String tablesInfo, String hints, Map<String, Object> promptMap,
                            String query, String modelId, List<Object> sqlExamples,
                            List<Object> nerExamples, String dialect) {
        log.info("[Bedrock] textToSql: model={}, query={}, dialect={}", resolveModel(modelId), truncate(query), dialect);

        SqlDialect sqlDialect = SqlDialect.fromValue(dialect);

        // 組裝 SQL 範例 (Python: example_sql_prompt)
        String exampleSqlPrompt = formatSqlExamples(sqlExamples);
        String exampleNerPrompt = formatNerExamples(nerExamples);

        // 組裝 dialect 顯示名稱
        String dialectDisplay = "redshift".equalsIgnoreCase(dialect) ? "Amazon Redshift" : dialect;

        // 建構 system prompt (含 {dialect} 替換)
        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.TEXT2SQL, modelId,
                Map.of("dialect", dialectDisplay != null ? dialectDisplay : "SQL"));

        // 建構 user prompt (含多個變數替換)
        Map<String, String> userVars = new LinkedHashMap<>();
        userVars.put("dialect_prompt", sqlDialect.getDialectPrompt());
        userVars.put("sql_schema", tablesInfo != null ? tablesInfo : "");
        userVars.put("examples", exampleSqlPrompt);
        userVars.put("ner_info", exampleNerPrompt);
        userVars.put("sql_guidance", hints != null ? hints : "");
        userVars.put("question", query);

        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.TEXT2SQL, modelId, userVars);

        return invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
    }

    @Override
    public String textToSqlWithCorrection(String tablesInfo, String hints, Map<String, Object> promptMap,
                                           String query, String modelId, List<Object> sqlExamples,
                                           List<Object> nerExamples, String dialect,
                                           String originalSql, String errorInfo) {
        log.info("[Bedrock] textToSqlWithCorrection: model={}, query={}, error={}", resolveModel(modelId), truncate(query), truncate(errorInfo));

        SqlDialect sqlDialect = SqlDialect.fromValue(dialect);
        String dialectDisplay = "redshift".equalsIgnoreCase(dialect) ? "Amazon Redshift" : dialect;

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.TEXT2SQL, modelId,
                Map.of("dialect", dialectDisplay != null ? dialectDisplay : "SQL"));

        Map<String, String> userVars = new LinkedHashMap<>();
        userVars.put("dialect_prompt", sqlDialect.getDialectPrompt());
        userVars.put("sql_schema", tablesInfo != null ? tablesInfo : "");
        userVars.put("examples", formatSqlExamples(sqlExamples));
        userVars.put("ner_info", formatNerExamples(nerExamples));
        userVars.put("sql_guidance", hints != null ? hints : "");
        userVars.put("question", query
                + "\n\nNOTE: when I try to write a SQL <sql>" + originalSql
                + "</sql>, I got an error <error>" + errorInfo
                + "</error>. Please consider and avoid this problem.");

        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.TEXT2SQL, modelId, userVars);

        return invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
    }

    @Override
    public String knowledgeSearch(String query, String modelId, Map<String, Object> promptMap) {
        log.info("[Bedrock] knowledgeSearch: model={}, query={}", resolveModel(modelId), truncate(query));

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.KNOWLEDGE, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.KNOWLEDGE, modelId,
                Map.of("question", query));

        return invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
    }

    @Override
    public String dataAnalyse(String modelId, Map<String, Object> promptMap,
                              String query, String dataJson, String type) {
        log.info("[Bedrock] dataAnalyse: model={}, query={}, type={}", resolveModel(modelId), truncate(query), type);

        // type 決定使用 agent_analyse 還是 data_summary
        PromptType promptType = "agent".equals(type) ? PromptType.AGENT_ANALYSE : PromptType.DATA_SUMMARY;

        String systemPrompt = promptService.buildSystemPrompt(promptMap, promptType, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, promptType, modelId,
                Map.of("question", query, "data", dataJson != null ? dataJson : ""));

        return invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
    }

    @Override
    public Map<String, Object> getAgentCotTask(String modelId, Map<String, Object> promptMap,
                                                String query, String tablesInfo, List<Object> agentExamples) {
        log.info("[Bedrock] getAgentCotTask: model={}, query={}", resolveModel(modelId), truncate(query));

        // 組裝 agent COT 範例
        String exampleData = formatAgentCotExamples(agentExamples);

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.AGENT, modelId,
                Map.of("table_schema_data", tablesInfo != null ? tablesInfo : "",
                        "sql_guidance", "",
                        "example_data", exampleData));
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.AGENT, modelId,
                Map.of("question", query));

        String response = invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public List<String> generateSuggestedQuestions(Map<String, Object> promptMap, String query, String modelId) {
        log.info("[Bedrock] generateSuggestedQuestions: model={}, query={}", resolveModel(modelId), truncate(query));

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.SUGGESTION, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.SUGGESTION, modelId,
                Map.of("question", query));

        String response = invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public Map<String, Object> dataVisualization(String modelId, String query,
                                                  List<Object> sqlData, Map<String, Object> promptMap) {
        log.info("[Bedrock] dataVisualization: model={}, query={}, dataSize={}", resolveModel(modelId), truncate(query), sqlData.size());

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.DATA_VISUALIZATION, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.DATA_VISUALIZATION, modelId,
                Map.of("question", query, "data", sqlData.toString()));

        String response = invokeModel(resolveModel(modelId), systemPrompt, userPrompt, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    // --- RAG 範例格式化 (Python: generate_llm_prompt 中的格式化邏輯) ---

    /**
     * 格式化 SQL 範例 — Python: example_sql_prompt 組裝邏輯
     */
    @SuppressWarnings("unchecked")
    private String formatSqlExamples(List<Object> sqlExamples) {
        if (sqlExamples == null || sqlExamples.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Object item : sqlExamples) {
            if (item instanceof Map<?, ?> map) {
                Object source = map.get("_source");
                if (source instanceof Map<?, ?> src) {
                    sb.append("Q: ").append(getOrEmpty(src, "text")).append("\n");
                    sb.append("A: ```sql\n").append(getOrEmpty(src, "sql")).append("```\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 格式化 NER 範例 — Python: example_ner_prompt 組裝邏輯
     */
    @SuppressWarnings("unchecked")
    private String formatNerExamples(List<Object> nerExamples) {
        if (nerExamples == null || nerExamples.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Object item : nerExamples) {
            if (item instanceof Map<?, ?> map) {
                Object source = map.get("_source");
                if (source instanceof Map<?, ?> src) {
                    sb.append("ner: ").append(getOrEmpty(src, "entity")).append("\n");
                    sb.append("ner info: ").append(getOrEmpty(src, "comment")).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 格式化 Agent COT 範例 — Python: agent_cot_example_str 組裝邏輯
     */
    @SuppressWarnings("unchecked")
    private String formatAgentCotExamples(List<Object> agentExamples) {
        if (agentExamples == null || agentExamples.isEmpty()) {
            return DEFAULT_AGENT_COT_EXAMPLE;
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : agentExamples) {
            if (item instanceof Map<?, ?> map) {
                Object source = map.get("_source");
                if (source instanceof Map<?, ?> src) {
                    sb.append("query: ").append(getOrEmpty(src, "query")).append("\n");
                    sb.append("train of thought: ").append(getOrEmpty(src, "comment")).append("\n");
                }
            }
        }
        return sb.isEmpty() ? DEFAULT_AGENT_COT_EXAMPLE : sb.toString();
    }

    // --- 內部工具 ---

    /** 呼叫 Bedrock Claude API (Messages API format) */
    String invokeModel(String modelId, String systemPrompt, String userMessage, int maxTokens) {
        try {
            Map<String, Object> body = Map.of(
                    "anthropic_version", "bedrock-2023-05-31",
                    "max_tokens", maxTokens,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userMessage)),
                    "temperature", bedrockProperties.temperature()
            );

            String bodyJson = objectMapper.writeValueAsString(body);
            log.debug("[Bedrock] Request body length: {}", bodyJson.length());

            InvokeModelResponse response = getClient().invokeModel(InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(bodyJson))
                    .build());

            String responseJson = response.body().asUtf8String();
            Map<String, Object> responseMap = objectMapper.readValue(responseJson, new TypeReference<>() {});

            // Claude 回傳格式: {"content": [{"type": "text", "text": "..."}]}
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
            String text = (String) content.getFirst().get("text");

            log.debug("[Bedrock] Response length: {}", text.length());
            return text;

        } catch (Exception e) {
            log.error("[Bedrock] invokeModel failed: model={}, error={}", modelId, e.getMessage(), e);
            throw new RuntimeException("Bedrock invocation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 懶初始化 Bedrock client (thread-safe double-check locking)
     */
    private BedrockRuntimeClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = BedrockRuntimeClient.builder()
                            .region(Region.of(bedrockProperties.region()))
                            .build();
                    log.info("[Bedrock] Client initialized: region={}", bedrockProperties.region());
                }
            }
        }
        return client;
    }

    /**
     * 解析模型 ID — 如果傳入為空則用預設
     */
    private String resolveModel(String modelId) {
        return (modelId != null && !modelId.isBlank()) ? modelId : bedrockProperties.modelId();
    }

    /**
     * 從 LLM 回應中解析 JSON — 支援 markdown code block 包裹
     */
    private <T> T parseJson(String response, TypeReference<T> typeRef) {
        try {
            // 去除 markdown code block 包裹
            String cleaned = response.strip();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.strip();

            // 嘗試找 JSON 區塊
            int jsonStart = cleaned.indexOf('{');
            int arrayStart = cleaned.indexOf('[');
            int start = -1;
            if (jsonStart >= 0 && (arrayStart < 0 || jsonStart < arrayStart)) {
                start = jsonStart;
            } else if (arrayStart >= 0) {
                start = arrayStart;
            }

            if (start > 0) {
                cleaned = cleaned.substring(start);
            }

            return objectMapper.readValue(cleaned, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("[Bedrock] Failed to parse JSON from response: {}", truncate(response), e);
            throw new RuntimeException("Failed to parse LLM response as JSON", e);
        }
    }

    /** 從 Map<?, ?> 安全取值，找不到回空字串 */
    private Object getOrEmpty(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val : "";
    }

    private String truncate(String s) {
        return s != null && s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }
}
