package com.nlq.service.mock;

import com.nlq.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 資料庫服務 Mock 實作 - 真的連 MySQL 執行 SQL（僅 dev 環境）
 * Mock database service — actually connects to MySQL and executes SQL (dev profile only)
 *
 * 這個不是完全 mock，而是用 Spring DataSource 真的去跑 SQL
 * This is not a pure mock — it actually runs SQL via Spring DataSource
 *
 * 對應 Python: utils/apis.py get_sql_result_tool()
 */
@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class MockDatabaseService implements DatabaseService {

    // Optional 注入：測試時無 DB 連線則 fallback 到 mock 資料
    // Optional injection — falls back to mock data when no DataSource bean exists
    private final Optional<DataSource> dataSource;

    /**
     * 執行 SQL 並回傳結果
     * Execute SQL and return results
     *
     * Python 原始回傳格式:
     * {"data": DataFrame/list, "sql": sql, "status_code": 200|500, "error_info": "..."}
     *
     * Java 回傳格式 (用 List<Map> 代替 DataFrame):
     * {"statusCode": 200|500, "data": [header_row, ...data_rows], "sql": sql, "errorInfo": "..."}
     */
    @Override
    public Map<String, Object> executeSql(Map<String, Object> databaseProfile, String sql) {
        log.info("[DB] Executing SQL: {}", sql);

        if (sql == null || sql.isBlank()) {
            return Map.of(
                    "statusCode", 500,
                    "data", List.of(),
                    "sql", "",
                    "errorInfo", "SQL is empty"
            );
        }

        if (dataSource.isEmpty()) {
            log.warn("[DB] No DataSource configured, returning mock data");
            return getMockResult(sql);
        }

        try (Connection conn = dataSource.get().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // 第一行是欄位名 (跟 Python 的 DataFrame 格式一致)
            // First row is column headers (matches Python's DataFrame format)
            List<String> headers = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                headers.add(meta.getColumnLabel(i));
            }

            List<Object> data = new ArrayList<>();
            data.add(headers); // header row

            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                data.add(row);
            }

            log.info("[DB] Query returned {} rows", data.size() - 1);
            return Map.of(
                    "statusCode", 200,
                    "data", data,
                    "sql", sql,
                    "errorInfo", ""
            );

        } catch (SQLException e) {
            log.error("[DB] SQL execution error: {}", e.getMessage());
            return Map.of(
                    "statusCode", 500,
                    "data", List.of(),
                    "sql", sql,
                    "errorInfo", e.getMessage()
            );
        }
    }

    /**
     * 沒有 DataSource 時的純假資料
     * Pure mock data when no DataSource is available
     */
    private Map<String, Object> getMockResult(String sql) {
        List<Object> data = new ArrayList<>();
        data.add(List.of("id", "product_name", "amount", "created_at"));
        data.add(List.of(1, "Laptop", 1200.00, "2025-01-15"));
        data.add(List.of(2, "Phone", 800.00, "2025-01-16"));
        data.add(List.of(3, "Tablet", 500.00, "2025-01-17"));

        return Map.of(
                "statusCode", 200,
                "data", data,
                "sql", sql,
                "errorInfo", ""
        );
    }
}
