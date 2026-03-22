package com.lndata.genbi.service;

import java.util.List;
import java.util.Map;

/**
 * Profile 管理服務介面
 */
public interface ProfileService {

    /** 取得所有 profile 及其詳細資訊 */
    Map<String, Map<String, Object>> getAllProfiles();

    /** 取得用戶的查詢歷史 */
    List<String> getHistoryBySession(String profileName, String userId, String sessionId, int size);

    /** 儲存查詢記錄 */
    void addLog(String logId, String userId, String sessionId, String profileName,
                String sql, String query, String intent, String logInfo);
}
