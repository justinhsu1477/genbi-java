package com.lndata.genbi.service;

import java.util.List;
import java.util.Map;

/**
 * 資料庫服務介面 — 執行 SQL 查詢
 */
public interface DatabaseService {

    /**
     * 執行 SQL 並回傳結果
     *
     * @param databaseProfile 資料庫連線資訊
     * @param sql             要執行的 SQL
     * @return {"statusCode": 200|500, "data": [...rows], "errorInfo": "...", "sql": "..."}
     */
    Map<String, Object> executeSql(Map<String, Object> databaseProfile, String sql);
}
