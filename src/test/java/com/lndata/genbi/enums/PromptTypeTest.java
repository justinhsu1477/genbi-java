package com.lndata.genbi.enums;

import com.lndata.genbi.model.constant.PromptType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromptType 列舉測試
 */
class PromptTypeTest {

    @Test
    @DisplayName("應有 9 種 prompt 類型")
    void shouldHaveNineTypes() {
        assertEquals(9, PromptType.values().length);
    }

    @Test
    @DisplayName("text2sql 的必要變數正確")
    void text2sql_requiredVars() {
        PromptType type = PromptType.TEXT2SQL;
        assertEquals("text2sql", type.getMapKey());
        assertTrue(type.getRequiredSystemVars().contains("dialect"));
        assertTrue(type.getRequiredUserVars().contains("question"));
        assertTrue(type.getRequiredUserVars().contains("sql_schema"));
        assertTrue(type.getRequiredUserVars().contains("dialect_prompt"));
        assertTrue(type.getRequiredUserVars().contains("examples"));
        assertTrue(type.getRequiredUserVars().contains("ner_info"));
        assertTrue(type.getRequiredUserVars().contains("sql_guidance"));
    }

    @Test
    @DisplayName("fromMapKey 正確查找")
    void fromMapKey_findsCorrect() {
        assertEquals(PromptType.TEXT2SQL, PromptType.fromMapKey("text2sql"));
        assertEquals(PromptType.INTENT, PromptType.fromMapKey("intent"));
        assertEquals(PromptType.QUERY_REWRITE, PromptType.fromMapKey("query_rewrite"));
        assertEquals(PromptType.AGENT_ANALYSE, PromptType.fromMapKey("agent_analyse"));
        assertNull(PromptType.fromMapKey("nonexistent"));
    }

    @Test
    @DisplayName("buildDefaultPromptMap 包含所有 9 種類型")
    void buildDefaultPromptMap_containsAllTypes() {
        Map<String, Object> map = PromptType.buildDefaultPromptMap();
        assertEquals(9, map.size());
        assertTrue(map.containsKey("text2sql"));
        assertTrue(map.containsKey("intent"));
        assertTrue(map.containsKey("query_rewrite"));
        assertTrue(map.containsKey("knowledge"));
        assertTrue(map.containsKey("agent"));
        assertTrue(map.containsKey("agent_analyse"));
        assertTrue(map.containsKey("data_summary"));
        assertTrue(map.containsKey("data_visualization"));
        assertTrue(map.containsKey("suggestion"));
    }

    @Test
    @DisplayName("預設 prompt 不為空")
    void defaultPrompts_notBlank() {
        for (PromptType type : PromptType.values()) {
            assertNotNull(type.getDefaultSystemPrompt(), type.name() + " system prompt");
            assertNotNull(type.getDefaultUserPrompt(), type.name() + " user prompt");
            assertFalse(type.getDefaultSystemPrompt().isBlank(), type.name() + " system prompt blank");
            assertFalse(type.getDefaultUserPrompt().isBlank(), type.name() + " user prompt blank");
        }
    }

    @Test
    @DisplayName("預設 system prompt 包含必要變數佔位符")
    void defaultSystemPrompt_containsRequiredVars() {
        for (PromptType type : PromptType.values()) {
            for (String var : type.getRequiredSystemVars()) {
                assertTrue(type.getDefaultSystemPrompt().contains("{" + var + "}"),
                        type.name() + " system prompt missing {" + var + "}");
            }
        }
    }

    @Test
    @DisplayName("預設 user prompt 包含必要變數佔位符")
    void defaultUserPrompt_containsRequiredVars() {
        for (PromptType type : PromptType.values()) {
            for (String var : type.getRequiredUserVars()) {
                assertTrue(type.getDefaultUserPrompt().contains("{" + var + "}"),
                        type.name() + " user prompt missing {" + var + "}");
            }
        }
    }
}
