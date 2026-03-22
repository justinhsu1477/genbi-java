package com.lndata.genbi.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseServiceImpl 單元測試 — 使用 H2 in-memory DB 驗證動態連線 + SQL 執行
 */
class DatabaseServiceImplTest {

    DatabaseServiceImpl databaseService;

    /** H2 in-memory 連線資訊 */
    private Map<String, Object> h2Profile() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("db_url", "jdbc:h2:mem:testdb_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        profile.put("db_type", "h2");
        profile.put("db_username", "sa");
        profile.put("db_password", "");
        return profile;
    }

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseServiceImpl();
    }

    @Nested
    @DisplayName("executeSql — 基本驗證")
    class BasicValidationTests {

        @Test
        @DisplayName("SQL 為 null → 500 + errorInfo")
        void shouldRejectNullSql() {
            Map<String, Object> result = databaseService.executeSql(Map.of(), null);

            assertEquals(500, result.get("statusCode"));
            assertEquals("SQL is empty", result.get("errorInfo"));
        }

        @Test
        @DisplayName("SQL 為空白 → 500 + errorInfo")
        void shouldRejectBlankSql() {
            Map<String, Object> result = databaseService.executeSql(Map.of(), "   ");

            assertEquals(500, result.get("statusCode"));
            assertEquals("SQL is empty", result.get("errorInfo"));
        }

        @Test
        @DisplayName("db_url 未設定 → 500 + errorInfo")
        void shouldRejectMissingDbUrl() {
            Map<String, Object> result = databaseService.executeSql(Map.of(), "SELECT 1");

            assertEquals(500, result.get("statusCode"));
            assertEquals("Database URL is not configured", result.get("errorInfo"));
        }
    }

    @Nested
    @DisplayName("executeSql — H2 動態連線")
    class H2ExecutionTests {

        @Test
        @DisplayName("簡單 SELECT 查詢成功")
        @SuppressWarnings("unchecked")
        void shouldExecuteSimpleSelect() {
            Map<String, Object> profile = h2Profile();

            Map<String, Object> result = databaseService.executeSql(profile, "SELECT 1 AS val, 'hello' AS msg");

            assertEquals(200, result.get("statusCode"));
            assertEquals("", result.get("errorInfo"));

            List<Object> data = (List<Object>) result.get("data");
            assertEquals(2, data.size()); // header + 1 row

            List<String> headers = (List<String>) data.get(0);
            assertEquals(List.of("VAL", "MSG"), headers);

            List<Object> row = (List<Object>) data.get(1);
            assertEquals(1, row.get(0));
            assertEquals("hello", row.get(1));
        }

        @Test
        @DisplayName("CREATE TABLE + INSERT + SELECT 完整流程")
        @SuppressWarnings("unchecked")
        void shouldExecuteFullFlow() {
            Map<String, Object> profile = h2Profile();
            String dbUrl = (String) profile.get("db_url");

            // 先建表 + 插入資料（用同一個 DB URL）
            databaseService.executeSql(
                    profileWithUrl(dbUrl),
                    "SELECT 1" // dummy，觸發 H2 DB 建立
            );

            // H2 允許用 INIT 參數，但更簡單的方式是分步驟
            // 用 DriverManager 直接建表
            try (var conn = java.sql.DriverManager.getConnection(dbUrl, "sa", "");
                 var stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE orders (id INT, product VARCHAR(50), amount DECIMAL(10,2))");
                stmt.execute("INSERT INTO orders VALUES (1, 'Laptop', 1200.00)");
                stmt.execute("INSERT INTO orders VALUES (2, 'Phone', 800.00)");
            } catch (Exception e) {
                fail("Setup failed: " + e.getMessage());
            }

            // 執行查詢
            Map<String, Object> result = databaseService.executeSql(
                    profileWithUrl(dbUrl),
                    "SELECT product, amount FROM orders ORDER BY id"
            );

            assertEquals(200, result.get("statusCode"));
            List<Object> data = (List<Object>) result.get("data");
            assertEquals(3, data.size()); // header + 2 rows
            assertEquals(List.of("PRODUCT", "AMOUNT"), data.get(0));
        }

        @Test
        @DisplayName("SQL 語法錯誤 → 500 + errorInfo")
        void shouldReturnErrorOnBadSql() {
            Map<String, Object> result = databaseService.executeSql(h2Profile(), "INVALID SQL STATEMENT");

            assertEquals(500, result.get("statusCode"));
            assertFalse(((String) result.get("errorInfo")).isEmpty());
        }

        @Test
        @DisplayName("查詢不存在的表 → 500 + errorInfo")
        void shouldReturnErrorOnMissingTable() {
            Map<String, Object> result = databaseService.executeSql(h2Profile(), "SELECT * FROM nonexistent_table");

            assertEquals(500, result.get("statusCode"));
            assertFalse(((String) result.get("errorInfo")).isEmpty());
        }

        @Test
        @DisplayName("db_username / db_password 為 null 時仍可連線 (H2)")
        void shouldHandleNullCredentials() {
            Map<String, Object> profile = new HashMap<>();
            profile.put("db_url", "jdbc:h2:mem:testdb_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
            profile.put("db_type", "h2");
            // 不設定 username / password

            Map<String, Object> result = databaseService.executeSql(profile, "SELECT 1 AS val");

            assertEquals(200, result.get("statusCode"));
        }

        private Map<String, Object> profileWithUrl(String dbUrl) {
            Map<String, Object> profile = new HashMap<>();
            profile.put("db_url", dbUrl);
            profile.put("db_type", "h2");
            profile.put("db_username", "sa");
            profile.put("db_password", "");
            return profile;
        }
    }
}
