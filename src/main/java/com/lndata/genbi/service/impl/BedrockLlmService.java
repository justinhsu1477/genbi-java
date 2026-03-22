package com.lndata.genbi.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lndata.genbi.config.BedrockProperties;
import com.lndata.genbi.model.constant.PromptType;
import com.lndata.genbi.model.constant.SqlDialect;
import com.lndata.genbi.service.LlmService;
import com.lndata.genbi.service.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Bedrock LLM 服務 — 透過 Spring AI ChatClient 呼叫 Claude (qas/prod 環境)
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class BedrockLlmService implements LlmService {

    private final ChatClient chatClient;
    private final BedrockProperties bedrockProperties;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

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
        log.info("[Bedrock] getQueryIntent: query={}", truncate(query));

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.INTENT, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.INTENT, modelId,
                Map.of("question", query));

        String response = callLlm(systemPrompt, userPrompt);
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public Map<String, Object> getQueryRewrite(String modelId, String query,
                                                 Map<String, Object> promptMap, List<String> history) {
        log.info("[Bedrock] getQueryRewrite: query={}, historySize={}", truncate(query), history.size());

        String chatHistory = String.join("\n", history);
        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.QUERY_REWRITE, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.QUERY_REWRITE, modelId,
                Map.of("chat_history", chatHistory, "question", query));

        String response = callLlm(systemPrompt, userPrompt);
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public String textToSql(String tablesInfo, String hints, Map<String, Object> promptMap,
                            String query, String modelId, List<Object> sqlExamples,
                            List<Object> nerExamples, String dialect) {
        log.info("[Bedrock] textToSql: query={}, dialect={}", truncate(query), dialect);

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
        userVars.put("question", query);

        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.TEXT2SQL, modelId, userVars);
        return callLlm(systemPrompt, userPrompt);
    }

    @Override
    public String textToSqlWithCorrection(String tablesInfo, String hints, Map<String, Object> promptMap,
                                           String query, String modelId, List<Object> sqlExamples,
                                           List<Object> nerExamples, String dialect,
                                           String originalSql, String errorInfo) {
        log.info("[Bedrock] textToSqlWithCorrection: query={}, error={}", truncate(query), truncate(errorInfo));

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
        return callLlm(systemPrompt, userPrompt);
    }

    @Override
    public String knowledgeSearch(String query, String modelId, Map<String, Object> promptMap) {
        log.info("[Bedrock] knowledgeSearch: query={}", truncate(query));

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.KNOWLEDGE, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.KNOWLEDGE, modelId,
                Map.of("question", query));

        return callLlm(systemPrompt, userPrompt);
    }

    @Override
    public String dataAnalyse(String modelId, Map<String, Object> promptMap,
                              String query, String dataJson, String type) {
        log.info("[Bedrock] dataAnalyse: query={}, type={}", truncate(query), type);

        PromptType promptType = "agent".equals(type) ? PromptType.AGENT_ANALYSE : PromptType.DATA_SUMMARY;

        String systemPrompt = promptService.buildSystemPrompt(promptMap, promptType, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, promptType, modelId,
                Map.of("question", query, "data", dataJson != null ? dataJson : ""));

        return callLlm(systemPrompt, userPrompt);
    }

    @Override
    public Map<String, Object> getAgentCotTask(String modelId, Map<String, Object> promptMap,
                                                String query, String tablesInfo, List<Object> agentExamples) {
        log.info("[Bedrock] getAgentCotTask: query={}", truncate(query));

        String exampleData = formatAgentCotExamples(agentExamples);

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.AGENT, modelId,
                Map.of("table_schema_data", tablesInfo != null ? tablesInfo : "",
                        "sql_guidance", "",
                        "example_data", exampleData));
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.AGENT, modelId,
                Map.of("question", query));

        String response = callLlm(systemPrompt, userPrompt);
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public List<String> generateSuggestedQuestions(Map<String, Object> promptMap, String query, String modelId) {
        log.info("[Bedrock] generateSuggestedQuestions: query={}", truncate(query));

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.SUGGESTION, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.SUGGESTION, modelId,
                Map.of("question", query));

        String response = callLlm(systemPrompt, userPrompt);
        return parseJson(response, new TypeReference<>() {});
    }

    @Override
    public Map<String, Object> dataVisualization(String modelId, String query,
                                                  List<Object> sqlData, Map<String, Object> promptMap) {
        log.info("[Bedrock] dataVisualization: query={}, dataSize={}", truncate(query), sqlData.size());

        String systemPrompt = promptService.buildSystemPrompt(promptMap, PromptType.DATA_VISUALIZATION, modelId, Map.of());
        String userPrompt = promptService.buildUserPrompt(promptMap, PromptType.DATA_VISUALIZATION, modelId,
                Map.of("question", query, "data", sqlData.toString()));

        String response = callLlm(systemPrompt, userPrompt);
        return parseJson(response, new TypeReference<>() {});
    }

    // --- 核心呼叫 ---

    /**
     * 透過 Spring AI ChatClient 呼叫 LLM
     */
    String callLlm(String systemPrompt, String userMessage) {
        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            log.debug("[Bedrock] Response length: {}", content != null ? content.length() : 0);
            return content;
        } catch (Exception e) {
            log.error("[Bedrock] ChatClient call failed: {}", e.getMessage(), e);
            throw new RuntimeException("LLM invocation failed: " + e.getMessage(), e);
        }
    }

    // --- RAG 範例格式化 ---

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

    // --- 工具方法 ---

    private <T> T parseJson(String response, TypeReference<T> typeRef) {
        try {
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

    private Object getOrEmpty(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val : "";
    }

    private String truncate(String s) {
        return s != null && s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }
}
