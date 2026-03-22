package com.lndata.genbi.service;

/**
 * Row Level Security 服務介面 — 在 SQL 執行前注入用戶權限過濾
 *
 * 使用場景：不同用戶只能看到自己有權限的資料列
 * 例：業務 A 只看亞洲區訂單，業務 B 只看歐洲區
 *
 * 注意：需要 auth 系統提供 LoginUser 才能啟用
 * 目前先預留介面，等 Lnfusion auth 整合後再實作
 *
 * RLS 設定格式 (YAML):
 *   tables:
 *     - table_name: orders
 *       column_name: territory
 *       column_value: $login_user.territory
 *
 * 改寫效果：
 *   原始 SQL:  SELECT * FROM orders
 *   改寫後:    WITH orders AS (SELECT * FROM orders WHERE territory = 'Asia') SELECT * FROM orders
 */
public interface RlsService {

    /**
     * 對 SQL 套用 Row Level Security 過濾
     *
     * @param sql        LLM 生成的原始 SQL
     * @param rlsConfig  YAML 格式的 RLS 設定（來自 Profile）
     * @param username   當前登入用戶名（來自 auth）
     * @return 改寫後的 SQL（加上 CTE WHERE 條件）
     */
    String applyRowLevelSecurity(String sql, String rlsConfig, String username);

    /**
     * 檢查 RLS 設定是否合法
     *
     * @param rlsConfig YAML 格式的 RLS 設定
     * @return 是否合法
     */
    boolean validateRlsConfig(String rlsConfig);
}
