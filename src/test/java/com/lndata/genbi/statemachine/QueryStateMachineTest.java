package com.lndata.genbi.statemachine;

import com.lndata.genbi.model.dto.ProcessingContext;
import com.lndata.genbi.model.dto.Question;
import com.lndata.genbi.model.constant.QueryState;
import com.lndata.genbi.service.DatabaseService;
import com.lndata.genbi.service.LlmService;
import com.lndata.genbi.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 狀態機單元測試
 * State machine unit tests
 *
 * 用 Mockito mock 所有外部服務，專注測試狀態流轉邏輯
 * Mock all external services to focus on state transition logic
 */
class QueryStateMachineTest {

    private LlmService llmService;
    private DatabaseService databaseService;
    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        llmService = mock(LlmService.class);
        databaseService = mock(DatabaseService.class);
        retrievalService = mock(RetrievalService.class);
    }

    /**
     * 建立預設的測試上下文
     * Create default test ProcessingContext
     */
    private ProcessingContext defaultContext() {
        Map<String, Object> dbProfile = new HashMap<>();
        dbProfile.put("tables_info", "CREATE TABLE orders (id INT, amount DECIMAL)");
        dbProfile.put("hints", "");
        dbProfile.put("prompt_map", Map.of());
        dbProfile.put("db_type", "mysql");
        dbProfile.put("db_url", "jdbc:mysql://localhost/test");

        return new ProcessingContext(
                "What is total sales?",   // searchBox
                "",                        // queryRewrite
                "session-1",               // sessionId
                "user-1",                  // userId
                "testuser",                // username
                "test-profile",            // selectedProfile
                dbProfile,                 // databaseProfile
                "anthropic.claude-3-sonnet", // modelType
                true,                      // useRagFlag
                true,                      // intentNerRecognitionFlag
                true,                      // agentCotFlag
                true,                      // explainGenProcessFlag
                true,                      // visualizeResultsFlag
                false,                     // dataWithAnalyse
                false,                     // genSuggestedQuestionFlag
                true,                      // autoCorrection
                0,                         // contextWindow (0 = 不做 query rewrite)
                List.of(),                 // userQueryHistory
                "INITIAL",                 // previousState
                List.of(),                 // entityRetrieval
                Map.of()                   // entityUserSelect
        );
    }

    private QueryStateMachine createMachine(ProcessingContext ctx) {
        return new QueryStateMachine(ctx, llmService, databaseService, retrievalService);
    }

    // ============================================================
    // 初始狀態測試 Initial state tests
    // ============================================================

    @Nested
    @DisplayName("INITIAL 狀態 Initial state")
    class InitialStateTests {

        @Test
        @DisplayName("初始狀態應為 INITIAL / initial state should be INITIAL")
        void shouldStartAtInitialState() {
            var machine = createMachine(defaultContext());
            assertEquals(QueryState.INITIAL, machine.getState());
        }

        @Test
        @DisplayName("contextWindow=0 時跳過改寫，直接到意圖識別 / skip rewrite when contextWindow=0")
        void shouldSkipRewriteWhenNoContext() {
            // 設定意圖識別回傳 normal_search
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "normal_search", "slot", List.of()));

            var machine = createMachine(defaultContext());
            machine.executeCurrentState(); // INITIAL -> INTENT_RECOGNITION

            assertEquals(QueryState.INTENT_RECOGNITION, machine.getState());
            assertEquals("What is total sales?", machine.getAnswer().getQuery());
        }
    }

    // ============================================================
    // 意圖識別測試 Intent recognition tests
    // ============================================================

    @Nested
    @DisplayName("INTENT_RECOGNITION 意圖識別")
    class IntentRecognitionTests {

        @Test
        @DisplayName("normal_search 意圖 -> ENTITY_RETRIEVAL")
        void normalSearchIntent() {
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "normal_search", "slot", List.of()));

            var machine = createMachine(defaultContext());
            machine.transition(QueryState.INTENT_RECOGNITION);
            machine.executeCurrentState();

            assertEquals(QueryState.ENTITY_RETRIEVAL, machine.getState());
            assertEquals("normal_search", machine.getAnswer().getQueryIntent());
        }

        @Test
        @DisplayName("reject_search 意圖 -> REJECT_INTENT")
        void rejectSearchIntent() {
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "reject_search", "slot", List.of()));

            var machine = createMachine(defaultContext());
            machine.transition(QueryState.INTENT_RECOGNITION);
            machine.executeCurrentState();

            assertEquals(QueryState.REJECT_INTENT, machine.getState());
        }

        @Test
        @DisplayName("knowledge_search 意圖 -> KNOWLEDGE_SEARCH")
        void knowledgeSearchIntent() {
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "knowledge_search", "slot", List.of()));

            var machine = createMachine(defaultContext());
            machine.transition(QueryState.INTENT_RECOGNITION);
            machine.executeCurrentState();

            assertEquals(QueryState.KNOWLEDGE_SEARCH, machine.getState());
        }

        @Test
        @DisplayName("agent_search 意圖 (agentCotFlag=true) -> AGENT_TASK")
        void agentSearchIntent() {
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "agent_search", "slot", List.of()));

            var machine = createMachine(defaultContext());
            machine.transition(QueryState.INTENT_RECOGNITION);
            machine.executeCurrentState();

            assertEquals(QueryState.AGENT_TASK, machine.getState());
            assertEquals("agent_search", machine.getAnswer().getQueryIntent());
        }
    }

    // ============================================================
    // 正常搜索流程測試 Normal search flow tests
    // ============================================================

    @Nested
    @DisplayName("Normal Search 完整流程")
    class NormalSearchFlowTests {

        @Test
        @DisplayName("完整流程: INITIAL -> INTENT -> ENTITY -> QA -> SQL -> EXECUTE -> COMPLETE")
        void fullNormalSearchFlow() {
            // Mock 意圖識別 mock intent recognition
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "normal_search", "slot", List.of()));

            // Mock 實體檢索 (無同名實體) mock entity retrieval (no duplicates)
            when(retrievalService.entityRetrieveSearch(any(), any()))
                    .thenReturn(List.of());

            // Mock QA 檢索 mock QA retrieval
            when(retrievalService.qaRetrieveSearch(any(), any()))
                    .thenReturn(List.of());

            // Mock SQL 生成 mock SQL generation
            when(llmService.textToSql(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn("<sql>SELECT SUM(amount) FROM orders</sql>");

            // Mock SQL 執行成功 mock successful SQL execution
            when(databaseService.executeSql(any(), any()))
                    .thenReturn(Map.of(
                            "statusCode", 200,
                            "data", List.of(Map.of("total", 10000)),
                            "sql", "SELECT SUM(amount) FROM orders"
                    ));

            var machine = createMachine(defaultContext());

            // 逐步執行 step through
            machine.executeCurrentState(); // INITIAL -> INTENT_RECOGNITION
            assertEquals(QueryState.INTENT_RECOGNITION, machine.getState());

            machine.executeCurrentState(); // INTENT -> ENTITY_RETRIEVAL
            assertEquals(QueryState.ENTITY_RETRIEVAL, machine.getState());

            machine.executeCurrentState(); // ENTITY -> QA_RETRIEVAL
            assertEquals(QueryState.QA_RETRIEVAL, machine.getState());

            machine.executeCurrentState(); // QA -> SQL_GENERATION
            assertEquals(QueryState.SQL_GENERATION, machine.getState());

            machine.executeCurrentState(); // SQL -> EXECUTE_QUERY
            assertEquals(QueryState.EXECUTE_QUERY, machine.getState());

            machine.executeCurrentState(); // EXECUTE -> COMPLETE
            assertEquals(QueryState.COMPLETE, machine.getState());

            // 驗證最終結果 verify final answer
            var answer = machine.getAnswer();
            assertEquals("normal_search", answer.getQueryIntent());
            assertEquals("SELECT SUM(amount) FROM orders", answer.getSqlSearchResult().sql());
            assertNotNull(answer.getSqlSearchResult().sqlData());
            assertTrue(answer.getErrorLog().isEmpty());
        }

        @Test
        @DisplayName("SQL 執行失敗 -> ERROR")
        void sqlExecutionFails() {
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "normal_search", "slot", List.of()));
            when(retrievalService.entityRetrieveSearch(any(), any())).thenReturn(List.of());
            when(retrievalService.qaRetrieveSearch(any(), any())).thenReturn(List.of());
            when(llmService.textToSql(any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn("<sql>INVALID SQL</sql>");

            // autoCorrection=true 但仍然失敗 auto-correction enabled but still fails
            when(databaseService.executeSql(any(), any()))
                    .thenReturn(Map.of("statusCode", 500, "errorInfo", "Syntax error", "data", List.of()));

            // 用 autoCorrection=false 的 context 測試直接 ERROR
            ProcessingContext ctx = new ProcessingContext(
                    "bad query", "", "s1", "u1", "user", "profile",
                    defaultContext().databaseProfile(), "model",
                    true, true, true, true, true, false, false,
                    false, // autoCorrection = false
                    0, List.of(), "INITIAL", List.of(), Map.of()
            );

            var machine = createMachine(ctx);
            machine.transition(QueryState.EXECUTE_QUERY);
            // 先設定 intentSearchResult (透過 SQL_GENERATION)
            // 直接測 EXECUTE 需要 intentSearchResult 有 sql
            // 改為從 INITIAL 開始完整跑
            machine = createMachine(ctx);
            // INITIAL
            machine.executeCurrentState();
            // INTENT_RECOGNITION
            machine.executeCurrentState();
            // ENTITY_RETRIEVAL
            machine.executeCurrentState();
            // QA_RETRIEVAL
            machine.executeCurrentState();
            // SQL_GENERATION
            machine.executeCurrentState();
            // EXECUTE_QUERY -> ERROR
            machine.executeCurrentState();

            assertEquals(QueryState.ERROR, machine.getState());
            assertFalse(machine.getAnswer().getErrorLog().isEmpty());
        }
    }

    // ============================================================
    // 知識搜索測試 Knowledge search tests
    // ============================================================

    @Nested
    @DisplayName("Knowledge Search 知識搜索")
    class KnowledgeSearchTests {

        @Test
        @DisplayName("知識搜索流程 -> COMPLETE + knowledge_response")
        void knowledgeSearchFlow() {
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "knowledge_search", "slot", List.of()));
            when(llmService.knowledgeSearch(any(), any(), any()))
                    .thenReturn("The answer is 42.");

            var machine = createMachine(defaultContext());
            machine.executeCurrentState(); // INITIAL -> INTENT
            machine.executeCurrentState(); // INTENT -> KNOWLEDGE_SEARCH
            machine.executeCurrentState(); // KNOWLEDGE_SEARCH -> COMPLETE

            assertEquals(QueryState.COMPLETE, machine.getState());
            assertEquals("knowledge_search", machine.getAnswer().getQueryIntent());
            assertEquals("The answer is 42.", machine.getAnswer().getKnowledgeSearchResult().knowledgeResponse());
        }
    }

    // ============================================================
    // 拒絕意圖測試 Reject intent tests
    // ============================================================

    @Nested
    @DisplayName("Reject Intent 拒絕意圖")
    class RejectIntentTests {

        @Test
        @DisplayName("拒絕回答 -> COMPLETE + reject_search")
        void rejectIntentFlow() {
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "reject_search", "slot", List.of()));

            var machine = createMachine(defaultContext());
            machine.executeCurrentState(); // INITIAL
            machine.executeCurrentState(); // INTENT -> REJECT
            machine.executeCurrentState(); // REJECT -> COMPLETE

            assertEquals(QueryState.COMPLETE, machine.getState());
            assertEquals("reject_search", machine.getAnswer().getQueryIntent());
        }
    }

    // ============================================================
    // Query Rewrite 測試 Query rewrite tests
    // ============================================================

    @Nested
    @DisplayName("Query Rewrite 查詢改寫")
    class QueryRewriteTests {

        @Test
        @DisplayName("contextWindow > 0 時觸發改寫 / trigger rewrite when contextWindow > 0")
        void queryRewriteTriggered() {
            when(llmService.getQueryRewrite(any(), any(), any(), any()))
                    .thenReturn(Map.of("intent", "normal", "query", "What is the total sales amount?"));
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "normal_search", "slot", List.of()));

            ProcessingContext ctx = new ProcessingContext(
                    "total sales?", "", "s1", "u1", "user", "profile",
                    defaultContext().databaseProfile(), "model",
                    true, true, true, true, true, false, false, true,
                    5, // contextWindow = 5
                    List.of("user:previous question"),
                    "INITIAL", List.of(), Map.of()
            );

            var machine = createMachine(ctx);
            machine.executeCurrentState(); // INITIAL (does rewrite) -> INTENT_RECOGNITION

            assertEquals(QueryState.INTENT_RECOGNITION, machine.getState());
            assertEquals("What is the total sales amount?", machine.getAnswer().getQueryRewrite());
        }

        @Test
        @DisplayName("ask_in_reply 意圖 -> COMPLETE (LLM 反問)")
        void askInReplyIntent() {
            when(llmService.getQueryRewrite(any(), any(), any(), any()))
                    .thenReturn(Map.of("intent", "ask_in_reply", "query", "Could you clarify what time range?"));

            ProcessingContext ctx = new ProcessingContext(
                    "how much?", "", "s1", "u1", "user", "profile",
                    defaultContext().databaseProfile(), "model",
                    true, true, true, true, true, false, false, true,
                    5, List.of("user:show me sales"),
                    "INITIAL", List.of(), Map.of()
            );

            var machine = createMachine(ctx);
            machine.executeCurrentState(); // INITIAL (rewrite -> ask_in_reply) -> COMPLETE

            assertEquals(QueryState.COMPLETE, machine.getState());
            assertEquals("ask_in_reply", machine.getAnswer().getQueryIntent());
            assertEquals("Could you clarify what time range?",
                    machine.getAnswer().getAskRewriteResult().queryRewrite());
        }
    }

    // ============================================================
    // 實體消歧測試 Entity disambiguation tests
    // ============================================================

    @Nested
    @DisplayName("Entity Select 實體消歧")
    class EntitySelectTests {

        @Test
        @DisplayName("同名實體 -> ASK_ENTITY_SELECT -> COMPLETE (entity_select)")
        void sameNameEntityTriggersSelection() {
            when(llmService.getQueryIntent(any(), any(), any()))
                    .thenReturn(Map.of("intent", "normal_search", "slot", List.of()));

            // 回傳同名實體 return same-name entities
            Map<String, Object> entity1 = new HashMap<>();
            entity1.put("_score", 0.99);
            Map<String, Object> source1 = new HashMap<>();
            source1.put("entity", "Apple");
            source1.put("entity_count", 2);
            source1.put("entity_table_info", List.of(Map.of("table_name", "products")));
            entity1.put("_source", source1);

            when(retrievalService.entityRetrieveSearch(any(), any()))
                    .thenReturn(List.of(entity1));

            var machine = createMachine(defaultContext());
            machine.executeCurrentState(); // INITIAL -> INTENT
            machine.executeCurrentState(); // INTENT -> ENTITY_RETRIEVAL
            machine.executeCurrentState(); // ENTITY -> ASK_ENTITY_SELECT

            assertEquals(QueryState.ASK_ENTITY_SELECT, machine.getState());

            machine.executeCurrentState(); // ASK_ENTITY_SELECT -> COMPLETE
            assertEquals(QueryState.COMPLETE, machine.getState());
            assertEquals("entity_select", machine.getAnswer().getQueryIntent());
        }
    }

    // ============================================================
    // DTO Record 測試 DTO record tests
    // ============================================================

    @Nested
    @DisplayName("DTO Record 測試")
    class DtoTests {

        @Test
        @DisplayName("Question record 預設值 / Question default values")
        void questionDefaults() {
            var q = new Question("test query", null, "profile", null, null,
                    null, true, true, true, true, false, false,
                    -1, null, null, null, null);

            assertEquals("anthropic.claude-3-sonnet-20240229-v1:0", q.bedrockModelId());
            assertEquals("-1", q.sessionId());
            assertEquals("admin", q.userId());
            assertEquals("", q.username());
            assertEquals(5, q.contextWindow()); // -1 被修正為 5
        }

        @Test
        @DisplayName("SqlSearchResult.empty() 回傳空結果")
        void sqlSearchResultEmpty() {
            var result = com.lndata.genbi.model.dto.SqlSearchResult.empty();
            assertEquals("", result.sql());
            assertEquals("table", result.dataShowType());
            assertTrue(result.sqlData().isEmpty());
        }

        @Test
        @DisplayName("ProcessingContext.from() 正確轉換")
        void processingContextFromQuestion() {
            var q = new Question("test", "model-1", "profile-1", "sess-1", "user-1",
                    "testuser", true, true, false, true, false, true,
                    3, "", "entity_select", Map.of(), List.of());
            var ctx = ProcessingContext.from(q, Map.of(), List.of());

            assertEquals("test", ctx.searchBox());
            assertEquals("USER_SELECT_ENTITY", ctx.previousState());
            assertTrue(ctx.dataWithAnalyse());
        }
    }
}
