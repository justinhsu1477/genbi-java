package com.nlq.service.mock;

import com.nlq.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Profile 管理服務 Mock 實作（僅 dev 環境）
 * Mock profile service — returns a hardcoded demo profile (dev profile only)
 *
 * 對應 Python: ProfileManagement, ConnectionManagement, LogManagement
 */
@Slf4j
@Service
@Profile("dev")
public class MockProfileService implements ProfileService {

    /**
     * 回傳假的 profile 資料 (指向 Docker MySQL)
     * Return fake profile data (points to Docker MySQL)
     *
     * Python 原始: ProfileManagement.get_all_profiles_with_info()
     * 每個 profile 包含: tables_info (DDL), hints, prompt_map, db_url, db_type, conn_name 等
     */
    @Override
    public Map<String, Map<String, Object>> getAllProfiles() {
        Map<String, Object> demoProfile = new HashMap<>();
        demoProfile.put("tables_info",
                """
                CREATE TABLE products (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    product_name VARCHAR(100) NOT NULL,
                    category VARCHAR(50),
                    price DECIMAL(10,2)
                );

                CREATE TABLE orders (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    product_id INT,
                    amount DECIMAL(10,2),
                    quantity INT,
                    customer_name VARCHAR(100),
                    created_at DATETIME,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                );
                """);
        demoProfile.put("hints", "amount = price * quantity");
        demoProfile.put("prompt_map", Map.of());
        demoProfile.put("db_url", "jdbc:mysql://mysql:3306/nlq_demo");
        demoProfile.put("db_type", "mysql");
        demoProfile.put("conn_name", "demo-mysql");

        return Map.of("demo-profile", demoProfile);
    }

    /**
     * 回傳假的歷史查詢
     * Return fake query history
     *
     * Python 原始: LogManagement.get_history_by_session()
     */
    @Override
    public List<String> getHistoryBySession(String profileName, String userId, String sessionId, int size) {
        log.info("[Mock Profile] getHistoryBySession: user={}, session={}", userId, sessionId);
        return new ArrayList<>(); // 空歷史 empty history
    }

    /**
     * 假的記錄儲存 (只印 log)
     * Fake log save — just print log
     *
     * Python 原始: LogManagement.add_log_to_database()
     */
    @Override
    public void addLog(String logId, String userId, String sessionId, String profileName,
                       String sql, String query, String intent, String logInfo) {
        log.info("[Mock Profile] addLog: user={}, session={}, intent={}, query={}", userId, sessionId, intent, query);
    }
}
