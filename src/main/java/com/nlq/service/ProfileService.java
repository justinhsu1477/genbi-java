package com.nlq.service;

import java.util.List;
import java.util.Map;

/**
 * Profile 管理服務介面
 * Profile management service — manages database profiles and connection info
 *
 * 對應 Python 的 ProfileManagement, ConnectionManagement, LogManagement
 */
public interface ProfileService {

    /**
     * 取得所有 profile 及其詳細資訊
     * Get all profiles with their details (tables_info, hints, prompt_map, db_url, db_type, etc.)
     */
    Map<String, Map<String, Object>> getAllProfiles();

    /**
     * 取得用戶的查詢歷史
     * Get user's query history for a session
     */
    List<String> getHistoryBySession(String profileName, String userId, String sessionId, int size);

    /**
     * 儲存查詢記錄
     * Save query log to database
     */
    void addLog(String logId, String userId, String sessionId, String profileName,
                String sql, String query, String intent, String logInfo);
}
