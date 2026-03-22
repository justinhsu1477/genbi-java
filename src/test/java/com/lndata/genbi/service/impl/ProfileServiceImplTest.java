package com.lndata.genbi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lndata.genbi.model.entity.ChatMessage;
import com.lndata.genbi.model.entity.DbProfile;
import com.lndata.genbi.repository.ChatMessageRepository;
import com.lndata.genbi.repository.DbProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProfileServiceImpl 單元測試
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock DbProfileRepository profileRepository;
    @Mock ChatMessageRepository messageRepository;
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks ProfileServiceImpl profileService;

    private DbProfile buildProfile(String name, String promptMapJson) {
        DbProfile p = new DbProfile();
        p.setId(1L);
        p.setProfileName(name);
        p.setConnName("conn-" + name);
        p.setDbType("mysql");
        p.setDbUrl("jdbc:mysql://localhost:3306/test");
        p.setDbUsername("user");
        p.setDbPassword("pass");
        p.setTablesInfo("CREATE TABLE t1 (id INT)");
        p.setHints("hint");
        p.setComments("comment");
        p.setPromptMap(promptMapJson);
        p.setRlsEnabled(false);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private ChatMessage buildMessage(String query) {
        ChatMessage msg = new ChatMessage();
        msg.setId(1L);
        msg.setSessionId("s1");
        msg.setUserId("u1");
        msg.setProfileName("p1");
        msg.setQuery(query);
        msg.setQueryIntent("normal_search");
        msg.setCreatedAt(Instant.now());
        return msg;
    }

    @Nested
    @DisplayName("getAllProfiles")
    class GetAllProfilesTests {

        @Test
        @DisplayName("正常回傳所有 profile 的 Map 格式")
        void shouldReturnAllProfilesAsMap() {
            when(profileRepository.findAll()).thenReturn(List.of(
                    buildProfile("p1", null),
                    buildProfile("p2", "{\"text2sql\": \"Generate SQL\"}")
            ));

            Map<String, Map<String, Object>> result = profileService.getAllProfiles();

            assertEquals(2, result.size());
            assertTrue(result.containsKey("p1"));
            assertTrue(result.containsKey("p2"));

            // 驗證 Map 內容
            Map<String, Object> p1 = result.get("p1");
            assertEquals("mysql", p1.get("db_type"));
            assertEquals("jdbc:mysql://localhost:3306/test", p1.get("db_url"));
            assertEquals("CREATE TABLE t1 (id INT)", p1.get("tables_info"));
            assertEquals("hint", p1.get("hints"));
            assertEquals("user", p1.get("db_username"));
            assertEquals(Map.of(), p1.get("prompt_map")); // null → 空 map
        }

        @Test
        @DisplayName("prompt_map JSON 正確解析")
        void shouldParsePromptMapJson() {
            when(profileRepository.findAll()).thenReturn(List.of(
                    buildProfile("p1", "{\"text2sql\": \"Generate SQL for {query}\"}")
            ));

            Map<String, Map<String, Object>> result = profileService.getAllProfiles();
            Map<String, Object> promptMap = castToMap(result.get("p1").get("prompt_map"));

            assertEquals("Generate SQL for {query}", promptMap.get("text2sql"));
        }

        @Test
        @DisplayName("prompt_map JSON 格式錯誤 → fallback 空 map")
        void shouldFallbackOnInvalidPromptMapJson() {
            when(profileRepository.findAll()).thenReturn(List.of(
                    buildProfile("p1", "not valid json!!!")
            ));

            Map<String, Map<String, Object>> result = profileService.getAllProfiles();
            Map<String, Object> promptMap = castToMap(result.get("p1").get("prompt_map"));

            assertTrue(promptMap.isEmpty());
        }

        @Test
        @DisplayName("無 profile → 回傳空 map")
        void shouldReturnEmptyWhenNoProfiles() {
            when(profileRepository.findAll()).thenReturn(List.of());

            Map<String, Map<String, Object>> result = profileService.getAllProfiles();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getHistoryBySession")
    class GetHistoryTests {

        @Test
        @DisplayName("回傳歷史查詢字串，SQL 層 LIMIT")
        void shouldReturnHistoryLimitedBySize() {
            when(messageRepository.findByUserIdAndSessionIdAndProfileNameOrderByCreatedAtDesc(
                    "u1", "s1", "p1", PageRequest.of(0, 2)))
                    .thenReturn(List.of(
                            buildMessage("query3"),
                            buildMessage("query2")
                    ));

            List<String> history = profileService.getHistoryBySession("p1", "u1", "s1", 2);

            assertEquals(2, history.size());
            assertEquals("query3", history.get(0));
            assertEquals("query2", history.get(1));
        }

        @Test
        @DisplayName("無歷史 → 回傳空 list")
        void shouldReturnEmptyWhenNoHistory() {
            when(messageRepository.findByUserIdAndSessionIdAndProfileNameOrderByCreatedAtDesc(
                    "u1", "s1", "p1", PageRequest.of(0, 10)))
                    .thenReturn(List.of());

            List<String> history = profileService.getHistoryBySession("p1", "u1", "s1", 10);

            assertTrue(history.isEmpty());
        }
    }

    @Nested
    @DisplayName("addLog")
    class AddLogTests {

        @Test
        @DisplayName("正常儲存查詢記錄")
        void shouldSaveLog() {
            profileService.addLog("log1", "u1", "s1", "p1",
                    "SELECT 1", "show orders", "normal_search", "{}");

            verify(messageRepository).save(any(ChatMessage.class));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        return (Map<String, Object>) obj;
    }
}
