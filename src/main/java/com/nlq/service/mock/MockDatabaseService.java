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
 * 資料庫服務 Mock 實作 — 用 Spring DataSource 真的去跑 SQL（僅 dev 環境）
 */
@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class MockDatabaseService implements DatabaseService {

    // Optional 注入：無 DB 連線時 fallback 到 mock 資料
    private final Optional<DataSource> dataSource;

    /** 執行 SQL 並回傳結果 */
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

            // 第一行是欄位名（header row）
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

    /** 沒有 DataSource 時的純假資料 */
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
