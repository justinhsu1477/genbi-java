package com.nlq.service;

import java.util.List;
import java.util.Map;

/**
 * 資料庫服務介面 - 執行 SQL 查詢
 * Database service interface — execute SQL queries
 *
 * 對應 Python 的 utils/apis.py get_sql_result_tool()
 * Maps to Python's utils/apis.py get_sql_result_tool()
 */
public interface DatabaseService {

    /**
     * 執行 SQL 並回傳結果
     * Execute SQL and return results
     *
     * @param databaseProfile 資料庫連線資訊 database connection profile
     * @param sql             要執行的 SQL the SQL to execute
     * @return {"statusCode": 200|500, "data": [...rows], "errorInfo": "...", "sql": "..."}
     */
    Map<String, Object> executeSql(Map<String, Object> databaseProfile, String sql);
}
