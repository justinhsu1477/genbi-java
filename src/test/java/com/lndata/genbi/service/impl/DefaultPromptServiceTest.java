package com.lndata.genbi.service.impl;

import com.lndata.genbi.enums.PromptType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultPromptService 單元測試
 */
class DefaultPromptServiceTest {

    private DefaultPromptService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPromptService();
    }

    @Nested
    @DisplayName("buildSystemPrompt")
    class BuildSystemPromptTest {

        @Test
        @DisplayName("promptMap 為 null → 使用預設 prompt")
        void nullPromptMap_usesDefault() {
            String result = service.buildSystemPrompt(null, PromptType.INTENT, null, Map.of());
            assertTrue(result.contains("intent classifier"));
        }

        @Test
        @DisplayName("promptMap 有覆蓋值 → 使用覆蓋")
        void overriddenPrompt_usesOverride() {
            Map<String, Object> promptMap = Map.of(
                    "intent", Map.of("system_prompt", "Custom system prompt")
            );
            String result = service.buildSystemPrompt(promptMap, PromptType.INTENT, null, Map.of());
            assertEquals("Custom system prompt", result);
        }

        @Test
        @DisplayName("promptMap Python 格式 (model_id 巢狀) → 正確提取")
        void pythonFormat_extractsCorrectly() {
            Map<String, Object> promptMap = Map.of(
                    "intent", Map.of(
                            "system_prompt", Map.of(
                                    "sonnet-3-5-20240620v1-0", "Sonnet custom prompt",
                                    "haiku-20240307v1-0", "Haiku custom prompt"
                            )
                    )
            );
            // 用完整 Bedrock model ID 查找
            String result = service.buildSystemPrompt(promptMap, PromptType.INTENT,
                    "anthropic.claude-3-5-sonnet-20240620-v1:0", Map.of());
            assertEquals("Sonnet custom prompt", result);
        }

        @Test
        @DisplayName("text2sql system prompt 的 {dialect} 變數替換")
        void text2sql_dialectReplacement() {
            String result = service.buildSystemPrompt(null, PromptType.TEXT2SQL, null,
                    Map.of("dialect", "MySQL"));
            assertTrue(result.contains("MySQL"));
            assertFalse(result.contains("{dialect}"));
        }
    }

    @Nested
    @DisplayName("buildUserPrompt")
    class BuildUserPromptTest {

        @Test
        @DisplayName("intent user prompt 替換 {question}")
        void intent_replacesQuestion() {
            String result = service.buildUserPrompt(null, PromptType.INTENT, null,
                    Map.of("question", "上個月營收多少"));
            assertTrue(result.contains("上個月營收多少"));
            assertFalse(result.contains("{question}"));
        }

        @Test
        @DisplayName("query_rewrite user prompt 替換 {chat_history} 和 {question}")
        void queryRewrite_replacesBothVars() {
            String result = service.buildUserPrompt(null, PromptType.QUERY_REWRITE, null,
                    Map.of("chat_history", "user: hello\nassistant: hi", "question", "那亞洲呢"));
            assertTrue(result.contains("user: hello"));
            assertTrue(result.contains("那亞洲呢"));
            assertFalse(result.contains("{chat_history}"));
            assertFalse(result.contains("{question}"));
        }

        @Test
        @DisplayName("text2sql user prompt 替換所有 6 個變數")
        void text2sql_replacesAllVars() {
            Map<String, String> vars = Map.of(
                    "dialect_prompt", "MySQL expert prompt",
                    "sql_schema", "CREATE TABLE orders ...",
                    "examples", "Q: revenue\nA: SELECT ...",
                    "ner_info", "ner: 台積電\nner info: TSMC",
                    "sql_guidance", "amount = price * qty",
                    "question", "月營收"
            );
            String result = service.buildUserPrompt(null, PromptType.TEXT2SQL, null, vars);
            assertTrue(result.contains("MySQL expert prompt"));
            assertTrue(result.contains("CREATE TABLE orders"));
            assertTrue(result.contains("SELECT"));
            assertTrue(result.contains("台積電"));
            assertTrue(result.contains("amount = price"));
            assertTrue(result.contains("月營收"));
        }
    }

    @Nested
    @DisplayName("validatePrompt")
    class ValidatePromptTest {

        @Test
        @DisplayName("合法 prompt → 通過驗證")
        void validPrompt_passes() {
            assertTrue(service.validatePrompt(
                    "Expert in {dialect}",
                    "{dialect_prompt} {sql_schema} {examples} {ner_info} {sql_guidance} {question}",
                    PromptType.TEXT2SQL
            ));
        }

        @Test
        @DisplayName("缺少必要變數 → 驗證失敗")
        void missingVar_fails() {
            assertFalse(service.validatePrompt(
                    "Expert in SQL",  // 缺少 {dialect}
                    "{question}",
                    PromptType.TEXT2SQL
            ));
        }

        @Test
        @DisplayName("intent prompt 無 system 必要變數 → 通過")
        void intent_noSystemVars_passes() {
            assertTrue(service.validatePrompt(
                    "Any system prompt",
                    "Q: {question}",
                    PromptType.INTENT
            ));
        }
    }

    @Nested
    @DisplayName("replaceVariables")
    class ReplaceVariablesTest {

        @Test
        @DisplayName("null template → 回傳 null")
        void nullTemplate_returnsNull() {
            assertNull(service.replaceVariables(null, Map.of("key", "value")));
        }

        @Test
        @DisplayName("空 variables → 不替換")
        void emptyVars_noChange() {
            assertEquals("hello {name}", service.replaceVariables("hello {name}", Map.of()));
        }

        @Test
        @DisplayName("多個變數同時替換")
        void multipleVars_allReplaced() {
            String result = service.replaceVariables("Hello {name}, age={age}",
                    Map.of("name", "Justin", "age", "30"));
            assertEquals("Hello Justin, age=30", result);
        }

        @Test
        @DisplayName("null value → 替換為空字串")
        void nullValue_replacedWithEmpty() {
            java.util.HashMap<String, String> vars = new java.util.HashMap<>();
            vars.put("name", null);
            String result = service.replaceVariables("Hello {name}!", vars);
            assertEquals("Hello !", result);
        }
    }

    @Nested
    @DisplayName("extractTemplate")
    class ExtractTemplateTest {

        @Test
        @DisplayName("null promptMap → 回傳預設值")
        void nullMap_returnsDefault() {
            String result = service.extractTemplate(null, "text2sql", "system_prompt", null, "default");
            assertEquals("default", result);
        }

        @Test
        @DisplayName("簡化格式: 直接字串值")
        void simpleFormat_directString() {
            Map<String, Object> promptMap = Map.of(
                    "intent", Map.of("system_prompt", "My custom intent prompt")
            );
            String result = service.extractTemplate(promptMap, "intent", "system_prompt", null, "default");
            assertEquals("My custom intent prompt", result);
        }

        @Test
        @DisplayName("Python 格式: model_id 巢狀 Map")
        void pythonFormat_nestedModelMap() {
            Map<String, Object> promptMap = Map.of(
                    "text2sql", Map.of("system_prompt", Map.of(
                            "sonnet-3-5-20240620v1-0", "Sonnet text2sql",
                            "haiku-20240307v1-0", "Haiku text2sql"
                    ))
            );
            String result = service.extractTemplate(promptMap, "text2sql", "system_prompt",
                    "anthropic.claude-3-5-sonnet-20240620-v1:0", "default");
            assertEquals("Sonnet text2sql", result);
        }

        @Test
        @DisplayName("type 不在 map 中 → 回傳預設值")
        void typeMissing_returnsDefault() {
            Map<String, Object> promptMap = Map.of("other", Map.of());
            String result = service.extractTemplate(promptMap, "text2sql", "system_prompt", null, "default");
            assertEquals("default", result);
        }
    }

    @Test
    @DisplayName("getDefaultPromptMap 返回完整結構")
    void getDefaultPromptMap_returnsComplete() {
        Map<String, Object> map = service.getDefaultPromptMap();
        assertEquals(9, map.size());

        // 檢查每個 entry 都有 title、system_prompt、user_prompt
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            assertTrue(entry.getValue() instanceof Map, entry.getKey() + " should be Map");
            @SuppressWarnings("unchecked")
            Map<String, Object> typeMap = (Map<String, Object>) entry.getValue();
            assertTrue(typeMap.containsKey("title"), entry.getKey() + " missing title");
            assertTrue(typeMap.containsKey("system_prompt"), entry.getKey() + " missing system_prompt");
            assertTrue(typeMap.containsKey("user_prompt"), entry.getKey() + " missing user_prompt");
        }
    }
}
