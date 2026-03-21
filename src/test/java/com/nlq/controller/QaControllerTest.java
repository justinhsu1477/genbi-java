package com.nlq.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlq.config.BedrockProperties;
import com.nlq.dto.FeedbackRequest;
import com.nlq.dto.MessageResponse;
import com.nlq.dto.SessionResponse;
import com.nlq.exception.BusinessException;
import com.nlq.exception.GlobalExceptionHandler;
import com.nlq.service.FeedbackService;
import com.nlq.service.ProfileService;
import com.nlq.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QaController 整合測試 — 使用 MockMvc 驗證 REST endpoint
 * Integration tests for QaController using MockMvc
 */
@ExtendWith(MockitoExtension.class)
class QaControllerTest {

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @Mock SessionService sessionService;
    @Mock FeedbackService feedbackService;
    @Mock ProfileService profileService;
    @Mock BedrockProperties bedrockProperties;

    @InjectMocks QaController qaController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(qaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /qa/option")
    class OptionTests {

        @Test
        @DisplayName("回傳 profile 和模型列表")
        void shouldReturnOptions() throws Exception {
            when(profileService.getAllProfiles()).thenReturn(Map.of("demo", Map.of()));
            when(bedrockProperties.modelId()).thenReturn("anthropic.claude-3-5-sonnet-20241022-v2:0");

            mockMvc.perform(get("/qa/option"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.profiles", hasSize(1)))
                    .andExpect(jsonPath("$.data.profiles[0]").value("demo"))
                    .andExpect(jsonPath("$.data.bedrockModelIds", hasSize(3)))
                    .andExpect(jsonPath("$.data.defaultModelId").value("anthropic.claude-3-5-sonnet-20241022-v2:0"));
        }
    }

    @Nested
    @DisplayName("GET /qa/sessions")
    class GetSessionsTests {

        @Test
        @DisplayName("回傳用戶的 session 列表")
        void shouldReturnSessions() throws Exception {
            LocalDateTime now = LocalDateTime.of(2026, 3, 20, 12, 0);
            when(sessionService.getSessionsByUser("user1")).thenReturn(List.of(
                    new SessionResponse("s1", "user1", "demo", "test", now, now)
            ));

            mockMvc.perform(get("/qa/sessions").param("userId", "user1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].sessionId").value("s1"));
        }

        @Test
        @DisplayName("缺少 userId 回傳 400")
        void shouldReturn400WhenMissingUserId() throws Exception {
            mockMvc.perform(get("/qa/sessions"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Nested
    @DisplayName("GET /qa/sessions/{sessionId}")
    class GetMessagesTests {

        @Test
        @DisplayName("回傳 session 內訊息")
        void shouldReturnMessages() throws Exception {
            LocalDateTime now = LocalDateTime.of(2026, 3, 20, 12, 0);
            when(sessionService.getMessagesBySession("s1")).thenReturn(List.of(
                    new MessageResponse(1L, "show orders", "show orders", "normal_search",
                            "SELECT * FROM orders", "{}", "claude-3", now)
            ));

            mockMvc.perform(get("/qa/sessions/s1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].query").value("show orders"));
        }

        @Test
        @DisplayName("Session 不存在回傳 404")
        void shouldReturn404WhenNotFound() throws Exception {
            when(sessionService.getMessagesBySession("bad"))
                    .thenThrow(BusinessException.notFound("Session bad"));

            mockMvc.perform(get("/qa/sessions/bad"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message", containsString("not found")));
        }
    }

    @Nested
    @DisplayName("DELETE /qa/sessions/{sessionId}")
    class DeleteSessionTests {

        @Test
        @DisplayName("正常刪除回傳 200")
        void shouldDeleteSession() throws Exception {
            doNothing().when(sessionService).deleteSession("s1");

            mockMvc.perform(delete("/qa/sessions/s1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(sessionService).deleteSession("s1");
        }
    }

    @Nested
    @DisplayName("POST /qa/feedback")
    class FeedbackTests {

        @Test
        @DisplayName("正常提交 feedback")
        void shouldSubmitFeedback() throws Exception {
            FeedbackRequest req = new FeedbackRequest(
                    "s1", "user1", 1L, "upvote", "show orders", "SELECT 1", "good");

            mockMvc.perform(post("/qa/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(feedbackService).saveFeedback(any(FeedbackRequest.class));
        }

        @Test
        @DisplayName("缺少必要欄位回傳 400")
        void shouldReturn400WhenInvalid() throws Exception {
            // feedbackType 為空
            String body = """
                    {"sessionId": "s1", "userId": "user1", "feedbackType": ""}
                    """;

            mockMvc.perform(post("/qa/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("feedbackType 格式錯誤回傳 400")
        void shouldReturn400WhenInvalidType() throws Exception {
            String body = """
                    {"sessionId": "s1", "userId": "user1", "feedbackType": "invalid"}
                    """;

            mockMvc.perform(post("/qa/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }
}
