package com.nlq.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlq.config.BedrockProperties;
import com.nlq.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;

/**
 * AWS Bedrock LLM 服務 — 真實呼叫 Claude (qas/prod 環境)
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class BedrockLlmService implements LlmService {

    private final BedrockProperties bedrockProperties;
    private final ObjectMapper objectMapper;

    private volatile BedrockRuntimeClient client;

    // --- 公開介面實作 ---

    @Override
    public Map<String, Object> getQueryIntent(String modelId, String query, Map<String, Object> promptMap) {
        log.info("[Bedrock] getQueryIntent: model={}, query={}", resolveModel(modelId), truncate(query));

        String systemPrompt = extractPrompt(promptMap, "intent_prompt",
                "You are an intent classifier. Classify the user query into one of: normal_search, knowledge_search, agent_search, reject_search. Also extract entity slots.");
        String userMessage = "Classify the intent and extract entities from: " + query
                + "\nReturn JSON: {\"intent\": \"...\", \"slot\": [{\"entity\": \"...\", \"value\": \"...\"}]}";

        String response = invokeModel(resolveModel(modelId), systemPrompt, userMessage, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public Map<String, Object> getQueryRewrite(String modelId, String query,
                                                 Map<String, Object> promptMap, List<String> history) {
        log.info("[Bedrock] getQueryRewrite: model={}, query={}, historySize={}", resolveModel(modelId), truncate(query), history.size());

        String systemPrompt = extractPrompt(promptMap, "query_rewrite_prompt",
                "You are a query rewriter. Rewrite the user query based on conversation history for clarity.");
        String userMessage = "History:\n" + String.join("\n", history) + "\n\nCurrent query: " + query
                + "\nReturn JSON: {\"intent\": \"normal\"|\"ask_in_reply\", \"query\": \"rewritten query\"}";

        String response = invokeModel(resolveModel(modelId), systemPrompt, userMessage, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public String textToSql(String tablesInfo, String hints, Map<String, Object> promptMap,
                            String query, String modelId, List<Object> sqlExamples,
                            List<Object> nerExamples, String dialect) {
        log.info("[Bedrock] textToSql: model={}, query={}, dialect={}", resolveModel(modelId), truncate(query), dialect);

        String systemPrompt = extractPrompt(promptMap, "text_to_sql_prompt",
                "You are a SQL expert. Generate SQL based on the given table schema, hints, and examples. "
                + "Return the SQL wrapped in <sql></sql> tags. Dialect: " + dialect);

        StringBuilder userMsg = new StringBuilder();
        userMsg.append("## Table Schema\n").append(tablesInfo).append("\n\n");
        if (hints != null && !hints.isBlank()) {
            userMsg.append("## Hints\n").append(hints).append("\n\n");
        }
        if (!sqlExamples.isEmpty()) {
            userMsg.append("## SQL Examples\n").append(sqlExamples).append("\n\n");
        }
        if (!nerExamples.isEmpty()) {
            userMsg.append("## Entity Examples\n").append(nerExamples).append("\n\n");
        }
        userMsg.append("## Question\n").append(query);

        return invokeModel(resolveModel(modelId), systemPrompt, userMsg.toString(), bedrockProperties.maxTokens());
    }

    @Override
    public String textToSqlWithCorrection(String tablesInfo, String hints, Map<String, Object> promptMap,
                                           String query, String modelId, List<Object> sqlExamples,
                                           List<Object> nerExamples, String dialect,
                                           String originalSql, String errorInfo) {
        log.info("[Bedrock] textToSqlWithCorrection: model={}, query={}, error={}", resolveModel(modelId), truncate(query), truncate(errorInfo));

        String systemPrompt = extractPrompt(promptMap, "text_to_sql_prompt",
                "You are a SQL expert. Generate SQL based on the given table schema, hints, and examples. "
                + "Return the SQL wrapped in <sql></sql> tags. Dialect: " + dialect);

        StringBuilder userMsg = new StringBuilder();
        userMsg.append("## Table Schema\n").append(tablesInfo).append("\n\n");
        if (hints != null && !hints.isBlank()) {
            userMsg.append("## Hints\n").append(hints).append("\n\n");
        }
        if (!sqlExamples.isEmpty()) {
            userMsg.append("## SQL Examples\n").append(sqlExamples).append("\n\n");
        }
        if (!nerExamples.isEmpty()) {
            userMsg.append("## Entity Examples\n").append(nerExamples).append("\n\n");
        }
        userMsg.append("## Question\n").append(query).append("\n\n");
        userMsg.append("NOTE: when I try to write a SQL <sql>").append(originalSql)
                .append("</sql>, I got an error <error>").append(errorInfo)
                .append("</error>. Please consider and avoid this problem.");

        return invokeModel(resolveModel(modelId), systemPrompt, userMsg.toString(), bedrockProperties.maxTokens());
    }

    @Override
    public String knowledgeSearch(String query, String modelId, Map<String, Object> promptMap) {
        log.info("[Bedrock] knowledgeSearch: model={}, query={}", resolveModel(modelId), truncate(query));

        String systemPrompt = extractPrompt(promptMap, "knowledge_prompt",
                "You are a helpful assistant. Answer the user's question directly based on your knowledge.");
        return invokeModel(resolveModel(modelId), systemPrompt, query, bedrockProperties.maxTokens());
    }

    @Override
    public String dataAnalyse(String modelId, Map<String, Object> promptMap,
                              String query, String dataJson, String type) {
        log.info("[Bedrock] dataAnalyse: model={}, query={}, type={}", resolveModel(modelId), truncate(query), type);

        String systemPrompt = extractPrompt(promptMap, "data_analyse_prompt",
                "You are a data analyst. Analyze the query results and provide insights.");
        String userMessage = "Question: " + query + "\n\nData:\n" + dataJson + "\n\nProvide analysis and insights.";

        return invokeModel(resolveModel(modelId), systemPrompt, userMessage, bedrockProperties.maxTokens());
    }

    @Override
    public Map<String, Object> getAgentCotTask(String modelId, Map<String, Object> promptMap,
                                                String query, String tablesInfo, List<Object> agentExamples) {
        log.info("[Bedrock] getAgentCotTask: model={}, query={}", resolveModel(modelId), truncate(query));

        String systemPrompt = extractPrompt(promptMap, "agent_cot_prompt",
                "You are a task planner. Break down complex queries into sub-tasks. Each sub-task should be a simpler query.");
        String userMessage = "Tables:\n" + tablesInfo + "\n\nExamples:\n" + agentExamples
                + "\n\nBreak this into sub-tasks: " + query
                + "\nReturn JSON: {\"task_1\": \"...\", \"task_2\": \"...\"}";

        String response = invokeModel(resolveModel(modelId), systemPrompt, userMessage, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public List<String> generateSuggestedQuestions(Map<String, Object> promptMap, String query, String modelId) {
        log.info("[Bedrock] generateSuggestedQuestions: model={}, query={}", resolveModel(modelId), truncate(query));

        String systemPrompt = extractPrompt(promptMap, "suggest_question_prompt",
                "Generate 3 follow-up questions based on the user's query. Return a JSON array of strings.");
        String userMessage = "User asked: " + query + "\nReturn JSON array: [\"question1\", \"question2\", \"question3\"]";

        String response = invokeModel(resolveModel(modelId), systemPrompt, userMessage, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public Map<String, Object> dataVisualization(String modelId, String query,
                                                  List<Object> sqlData, Map<String, Object> promptMap) {
        log.info("[Bedrock] dataVisualization: model={}, query={}, dataSize={}", resolveModel(modelId), truncate(query), sqlData.size());

        String systemPrompt = extractPrompt(promptMap, "data_visualization_prompt",
                "You are a data visualization expert. Choose the best chart type for the data.");
        String userMessage = "Question: " + query + "\nData: " + sqlData
                + "\nReturn JSON: {\"showType\": \"table|chart\", \"chartType\": \"bar|line|pie|-1\", \"chartData\": [...]}";

        String response = invokeModel(resolveModel(modelId), systemPrompt, userMessage, bedrockProperties.maxTokens());
        return parseJson(response, new TypeReference<>() {});
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
     * 從 promptMap 提取 prompt，找不到就用預設值
     */
    private String extractPrompt(Map<String, Object> promptMap, String key, String defaultPrompt) {
        if (promptMap == null || !promptMap.containsKey(key)) {
            return defaultPrompt;
        }
        return String.valueOf(promptMap.get(key));
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

    private String truncate(String s) {
        return s != null && s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }
}
