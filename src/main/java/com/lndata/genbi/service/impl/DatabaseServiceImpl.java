package com.lndata.genbi.service.impl;

import com.lndata.genbi.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 資料庫服務正式實作 — 動態連線到客戶 DB 並執行 SQL
 */
@Slf4j
@Service
@Profile("!dev")
public class DatabaseServiceImpl implements DatabaseService {

    private static final int QUERY_TIMEOUT_SECONDS = 30;

    /**
     * 從 databaseProfile 取得連線資訊，動態建立連線並執行 SQL
     */
    @Override
    public Map<String, Object> executeSql(Map<String, Object> databaseProfile, String sql) {
        if (sql == null || sql.isBlank()) {
            return errorResult("", "SQL is empty");
        }

        String dbUrl = getString(databaseProfile, "db_url");
        String dbUser = getString(databaseProfile, "db_username");
        String dbPass = getString(databaseProfile, "db_password");

        if (dbUrl == null || dbUrl.isBlank()) {
            return errorResult(sql, "Database URL is not configured");
        }

        log.info("[DatabaseService] executeSql: url={}, sql={}", maskUrl(dbUrl), truncate(sql, 200));

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                // header row
                List<String> headers = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    headers.add(meta.getColumnLabel(i));
                }

                List<Object> data = new ArrayList<>();
                data.add(headers);

                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    data.add(row);
                }

                log.info("[DatabaseService] query returned {} rows", data.size() - 1);
                return Map.of(
                        "statusCode", 200,
                        "data", data,
                        "sql", sql,
                        "errorInfo", ""
                );
            }

        } catch (SQLException e) {
            log.error("[DatabaseService] SQL execution error: {}", e.getMessage());
            return errorResult(sql, e.getMessage());
        }
    }

    private Map<String, Object> errorResult(String sql, String errorInfo) {
        return Map.of(
                "statusCode", 500,
                "data", List.of(),
                "sql", sql,
                "errorInfo", errorInfo
        );
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    /** 遮蔽 JDBC URL 中的密碼部分 */
    private String maskUrl(String url) {
        return url.replaceAll("password=[^&]*", "password=***");
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
