package com.lndata.genbi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lndata.genbi.model.entity.ChatMessage;
import com.lndata.genbi.model.entity.DbProfile;
import com.lndata.genbi.repository.ChatMessageRepository;
import com.lndata.genbi.repository.DbProfileRepository;
import com.lndata.genbi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Profile 服務正式實作 — 從 DB 讀取 profile 資訊供狀態機使用
 */
@Slf4j
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final DbProfileRepository profileRepository;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    /** 從 DB 取得所有 profile，轉成狀態機需要的 Map 格式 */
    @Override
    public Map<String, Map<String, Object>> getAllProfiles() {
        List<DbProfile> profiles = profileRepository.findAll();
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        for (DbProfile p : profiles) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("tables_info", p.getTablesInfo());
            detail.put("hints", p.getHints());
            detail.put("db_url", p.getDbUrl());
            detail.put("db_type", p.getDbType());
            detail.put("db_username", p.getDbUsername());
            detail.put("db_password", p.getDbPassword());
            detail.put("conn_name", p.getConnName());
            detail.put("comments", p.getComments());
            detail.put("prompt_map", parsePromptMap(p.getPromptMap()));
            detail.put("rls_enabled", p.getRlsEnabled());
            detail.put("rls_config", p.getRlsConfig());
            result.put(p.getProfileName(), detail);
        }

        log.info("[ProfileService] getAllProfiles: loaded {} profiles", result.size());
        return result;
    }

    /** 從 ChatMessage 查詢歷史紀錄（SQL 層 LIMIT，不在 Java 層截斷） */
    @Override
    public List<String> getHistoryBySession(String profileName, String userId, String sessionId, int size) {
        List<ChatMessage> messages = messageRepository
                .findByUserIdAndSessionIdAndProfileNameOrderByCreatedAtDesc(
                        userId, sessionId, profileName, PageRequest.of(0, size));

        List<String> history = messages.stream()
                .map(ChatMessage::getQuery)
                .toList();

        log.info("[ProfileService] getHistoryBySession: user={}, session={}, found={}", userId, sessionId, history.size());
        return history;
    }

    /** 儲存查詢記錄到 ChatMessage */
    @Override
    public void addLog(String logId, String userId, String sessionId, String profileName,
                       String sql, String query, String intent, String logInfo) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setProfileName(profileName);
        message.setQuery(query);
        message.setSqlText(sql);
        message.setQueryIntent(intent);
        message.setAnswer(logInfo);

        messageRepository.save(message);
        log.info("[ProfileService] addLog: user={}, session={}, intent={}", userId, sessionId, intent);
    }

    /** 解析 prompt_map JSON 字串為 Map */
    private Map<String, Object> parsePromptMap(String promptMapJson) {
        if (promptMapJson == null || promptMapJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(promptMapJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[ProfileService] parsePromptMap failed: {}", e.getMessage());
            return Map.of();
        }
    }
}
