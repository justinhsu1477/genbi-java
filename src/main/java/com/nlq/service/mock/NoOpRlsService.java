package com.nlq.service.mock;

import com.nlq.service.RlsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RLS 空實作 — auth 系統未接入前，不做任何 SQL 改寫
 */
@Slf4j
@Service
public class NoOpRlsService implements RlsService {

    @Override
    public String applyRowLevelSecurity(String sql, String rlsConfig, String username) {
        // auth 未接入，直接回傳原始 SQL
        log.debug("[RLS] NoOp — SQL passed through without RLS (auth not integrated)");
        return sql;
    }

    @Override
    public boolean validateRlsConfig(String rlsConfig) {
        return rlsConfig != null && !rlsConfig.isBlank();
    }
}
