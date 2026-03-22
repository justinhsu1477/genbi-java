package com.nlq.service;

import com.nlq.dto.MessageResponse;
import com.nlq.dto.SessionResponse;
import com.nlq.entity.ChatMessage;
import com.nlq.entity.ChatSession;
import com.nlq.exception.BusinessException;
import com.nlq.repository.ChatMessageRepository;
import com.nlq.repository.ChatSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SessionService 單元測試
 * Unit tests for SessionService
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    ChatSessionRepository sessionRepository;
    @Mock
    ChatMessageRepository messageRepository;

    @InjectMocks
    SessionService sessionService;

    private ChatSession buildSession(String sessionId, String userId) {
        return ChatSession.builder()
                .id(1L)
                .sessionId(sessionId)
                .userId(userId)
                .profileName("demo")
                .title("test query")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ChatMessage buildMessage(String sessionId, String query) {
        return ChatMessage.builder()
                .id(1L)
                .sessionId(sessionId)
                .userId("user1")
                .profileName("demo")
                .query(query)
                .queryIntent("normal_search")
                .sqlText("SELECT 1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getSessionsByUser 查詢 Session 列表")
    class GetSessionsTests {

        @Test
        @DisplayName("正常回傳 session 列表")
        void shouldReturnSessions() {
            when(sessionRepository.findByUserIdOrderByUpdatedAtDesc("user1"))
                    .thenReturn(List.of(buildSession("s1", "user1"), buildSession("s2", "user1")));

            List<SessionResponse> result = sessionService.getSessionsByUser("user1");

            assertEquals(2, result.size());
            assertEquals("s1", result.get(0).sessionId());
        }

        @Test
        @DisplayName("無 session 回傳空列表")
        void shouldReturnEmptyList() {
            when(sessionRepository.findByUserIdOrderByUpdatedAtDesc("user2"))
                    .thenReturn(List.of());

            List<SessionResponse> result = sessionService.getSessionsByUser("user2");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getMessagesBySession 查詢訊息")
    class GetMessagesTests {

        @Test
        @DisplayName("正常回傳訊息列表")
        void shouldReturnMessages() {
            when(sessionRepository.findBySessionId("s1"))
                    .thenReturn(Optional.of(buildSession("s1", "user1")));
            when(messageRepository.findBySessionIdOrderByCreatedAtAsc("s1"))
                    .thenReturn(List.of(buildMessage("s1", "show orders")));

            List<MessageResponse> result = sessionService.getMessagesBySession("s1");

            assertEquals(1, result.size());
            assertEquals("show orders", result.get(0).query());
        }

        @Test
        @DisplayName("Session 不存在拋 BusinessException")
        void shouldThrowWhenSessionNotFound() {
            when(sessionRepository.findBySessionId("invalid"))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> sessionService.getMessagesBySession("invalid"));
            assertEquals(404, ex.getCode());
        }
    }

    @Nested
    @DisplayName("deleteSession 刪除 Session")
    class DeleteSessionTests {

        @Test
        @DisplayName("正常刪除 session 及訊息")
        void shouldDeleteSessionAndMessages() {
            when(sessionRepository.findBySessionId("s1"))
                    .thenReturn(Optional.of(buildSession("s1", "user1")));

            sessionService.deleteSession("s1");

            verify(messageRepository).deleteBySessionId("s1");
            verify(sessionRepository).deleteBySessionId("s1");
        }

        @Test
        @DisplayName("Session 不存在拋 BusinessException")
        void shouldThrowWhenNotFound() {
            when(sessionRepository.findBySessionId("bad"))
                    .thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> sessionService.deleteSession("bad"));
            verify(messageRepository, never()).deleteBySessionId(any());
        }
    }

    @Nested
    @DisplayName("saveMessage 儲存訊息")
    class SaveMessageTests {

        @Test
        @DisplayName("Session 存在 — 儲存訊息並更新 timestamp")
        void shouldSaveMessageToExistingSession() {
            ChatSession existing = buildSession("s1", "user1");
            when(sessionRepository.findBySessionId("s1")).thenReturn(Optional.of(existing));
            when(sessionRepository.save(any())).thenReturn(existing);

            sessionService.saveMessage("s1", "user1", "demo",
                    "show orders", "show orders", "normal_search",
                    "SELECT * FROM orders", "{}", "claude-3");

            verify(messageRepository).save(any(ChatMessage.class));
            verify(sessionRepository).save(existing);
        }

        @Test
        @DisplayName("Session 不存在 — 自動建立新 session")
        void shouldCreateNewSession() {
            when(sessionRepository.findBySessionId("new-session")).thenReturn(Optional.empty());
            when(sessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

            sessionService.saveMessage("new-session", "user1", "demo",
                    "show orders", "show orders", "normal_search",
                    "SELECT 1", "{}", "claude-3");

            ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
            verify(sessionRepository, times(2)).save(captor.capture());
            assertEquals("new-session", captor.getAllValues().get(0).getSessionId());
            verify(messageRepository).save(any(ChatMessage.class));
        }
    }
}
