package com.nlq.statemachine;

import com.nlq.dto.ProcessingContext;
import com.nlq.enums.QueryState;
import com.nlq.service.DatabaseService;
import com.nlq.service.LlmService;
import com.nlq.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Auto Correction（SQL 自動修正）測試
 */
@ExtendWith(MockitoExtension.class)
class AutoCorrectionTest {

    @Mock LlmService llmService;
    @Mock DatabaseService databaseService;
    @Mock RetrievalService retrievalService;

    QueryStateMachine stateMachine;

    @BeforeEach
    void setup() {
        Map<String, Object> dbProfile = new HashMap<>();
        dbProfile.put("tables_info", "CREATE TABLE orders (id INT, amount DECIMAL)");
        dbProfile.put("hints", "amount = price * quantity");
        dbProfile.put("db_type", "mysql");
        dbProfile.put("prompt_map", Map.of());

        ProcessingContext ctx = new ProcessingContext(
                "show total orders", "show total orders", "s1", "u1", "test",
                "demo", dbProfile, "claude-3", false, false, false, false,
                true, true, false, true,  // autoCorrection = true
                0, List.of(), QueryState.INITIAL.name(), List.of(), Map.of()
        );

        stateMachine = new QueryStateMachine(ctx, llmService, databaseService, retrievalService);
    }

    @Test
    @DisplayName("SQL 執行失敗 → Auto Correction 成功 → ANALYZE_DATA")
    void shouldAutoCorrectAndContinue() {
        // 模擬第一次 SQL 生成
        when(llmService.textToSql(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<sql>SELECT INVALID FROM orders</sql>");

        // 第一次執行失敗
        Map<String, Object> failResult = new HashMap<>();
        failResult.put("statusCode", 500);
        failResult.put("errorInfo", "Unknown column 'INVALID'");
        failResult.put("data", List.of());

        // 修正後的 SQL
        when(llmService.textToSqlWithCorrection(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<sql>SELECT COUNT(*) FROM orders</sql>");

        // 第二次執行成功
        Map<String, Object> successResult = new HashMap<>();
        successResult.put("statusCode", 200);
        successResult.put("data", List.of(Map.of("count", 42)));

        when(databaseService.executeSql(any(), eq("SELECT INVALID FROM orders"))).thenReturn(failResult);
        when(databaseService.executeSql(any(), eq("SELECT COUNT(*) FROM orders"))).thenReturn(successResult);

        // 執行流程：SQL_GENERATION → EXECUTE_QUERY → Auto Correction → ANALYZE_DATA
        stateMachine.transition(QueryState.SQL_GENERATION);
        stateMachine.executeCurrentState(); // SQL_GENERATION → EXECUTE_QUERY
        stateMachine.executeCurrentState(); // EXECUTE_QUERY → auto correct → ANALYZE_DATA

        assertEquals(QueryState.ANALYZE_DATA, stateMachine.getState());
        assertEquals("SELECT COUNT(*) FROM orders", stateMachine.getAnswer().getSqlSearchResult().sql());
    }

    @Test
    @DisplayName("SQL 執行失敗 → Auto Correction 也失敗 → ERROR")
    void shouldErrorWhenCorrectionAlsoFails() {
        when(llmService.textToSql(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<sql>BAD SQL</sql>");

        Map<String, Object> failResult = new HashMap<>();
        failResult.put("statusCode", 500);
        failResult.put("errorInfo", "Syntax error");
        failResult.put("data", List.of());

        // 修正後的 SQL 也失敗
        when(llmService.textToSqlWithCorrection(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<sql>STILL BAD SQL</sql>");

        Map<String, Object> stillFail = new HashMap<>();
        stillFail.put("statusCode", 500);
        stillFail.put("errorInfo", "Still syntax error");
        stillFail.put("data", List.of());

        when(databaseService.executeSql(any(), eq("BAD SQL"))).thenReturn(failResult);
        when(databaseService.executeSql(any(), eq("STILL BAD SQL"))).thenReturn(stillFail);

        stateMachine.transition(QueryState.SQL_GENERATION);
        stateMachine.executeCurrentState(); // SQL_GENERATION
        stateMachine.executeCurrentState(); // EXECUTE_QUERY → auto correct → still fail → ERROR

        assertEquals(QueryState.ERROR, stateMachine.getState());
        assertTrue(stateMachine.getAnswer().getErrorLog().containsKey("EXECUTE_QUERY"));
    }

    @Test
    @DisplayName("autoCorrection 關閉 → SQL 失敗直接 ERROR")
    void shouldErrorDirectlyWhenCorrectionDisabled() {
        // 建立 autoCorrection = false 的 context
        Map<String, Object> dbProfile = new HashMap<>();
        dbProfile.put("tables_info", "CREATE TABLE orders (id INT)");
        dbProfile.put("hints", "");
        dbProfile.put("db_type", "mysql");
        dbProfile.put("prompt_map", Map.of());

        ProcessingContext noAutoCtx = new ProcessingContext(
                "query", "query", "s1", "u1", "test",
                "demo", dbProfile, "claude-3", false, false, false, false,
                true, false, false, false,  // autoCorrection = false
                0, List.of(), QueryState.INITIAL.name(), List.of(), Map.of()
        );

        QueryStateMachine sm = new QueryStateMachine(noAutoCtx, llmService, databaseService, retrievalService);

        when(llmService.textToSql(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<sql>BAD SQL</sql>");

        Map<String, Object> failResult = new HashMap<>();
        failResult.put("statusCode", 500);
        failResult.put("errorInfo", "Error");
        failResult.put("data", List.of());

        when(databaseService.executeSql(any(), any())).thenReturn(failResult);

        sm.transition(QueryState.SQL_GENERATION);
        sm.executeCurrentState(); // SQL_GENERATION
        sm.executeCurrentState(); // EXECUTE_QUERY → no auto correct → ERROR

        assertEquals(QueryState.ERROR, sm.getState());
        // 不應該呼叫 textToSqlWithCorrection
        verify(llmService, never()).textToSqlWithCorrection(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
