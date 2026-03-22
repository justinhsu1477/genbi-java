package com.lndata.genbi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lndata.genbi.model.dto.ProfileRequest;
import com.lndata.genbi.model.dto.ProfileResponse;
import com.lndata.genbi.exception.BusinessException;
import com.lndata.genbi.exception.GlobalExceptionHandler;
import com.lndata.genbi.service.DbProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProfileController MockMvc 測試
 */
@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @Mock DbProfileService profileService;
    @InjectMocks ProfileController profileController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ProfileResponse buildResponse(String name) {
        Instant now = Instant.parse("2026-03-22T04:00:00Z");
        return new ProfileResponse(1L, name, "conn", "mysql",
                "jdbc:mysql://localhost/db", "CREATE TABLE t1 (id INT)", "hint", "comment",
                null, false, now, now);
    }

    @Test
    @DisplayName("GET /qa/profiles — 取得所有 profile")
    void shouldListProfiles() throws Exception {
        when(profileService.getAllProfiles()).thenReturn(List.of(buildResponse("p1")));

        mockMvc.perform(get("/qa/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].profileName").value("p1"));
    }

    @Test
    @DisplayName("GET /qa/profiles/{name} — 取得單一 profile")
    void shouldGetProfile() throws Exception {
        when(profileService.getByName("p1")).thenReturn(buildResponse("p1"));

        mockMvc.perform(get("/qa/profiles/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileName").value("p1"))
                .andExpect(jsonPath("$.data.dbType").value("mysql"));
    }

    @Test
    @DisplayName("GET /qa/profiles/{name} — 404")
    void shouldReturn404() throws Exception {
        when(profileService.getByName("bad")).thenThrow(BusinessException.notFound("Profile bad"));

        mockMvc.perform(get("/qa/profiles/bad"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /qa/profiles — 建立 profile")
    void shouldCreateProfile() throws Exception {
        ProfileRequest req = new ProfileRequest("new-p", "conn", "mysql",
                "jdbc:mysql://localhost/db", null, null, "DDL", "hint", null, null, false, null);
        when(profileService.create(any())).thenReturn(buildResponse("new-p"));

        mockMvc.perform(post("/qa/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileName").value("new-p"));
    }

    @Test
    @DisplayName("POST /qa/profiles — 缺少必填欄位 400")
    void shouldReturn400WhenMissing() throws Exception {
        String body = """
                {"profileName": "", "dbType": "mysql", "dbUrl": "jdbc:mysql://localhost/db"}
                """;

        mockMvc.perform(post("/qa/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /qa/profiles/{name} — 更新 profile")
    void shouldUpdateProfile() throws Exception {
        ProfileRequest req = new ProfileRequest("p1", "conn2", "oracle",
                "jdbc:oracle:thin:@localhost:1521/xe", null, null, "DDL2", "hint2", null, null, false, null);
        when(profileService.update(eq("p1"), any())).thenReturn(
                new ProfileResponse(1L, "p1", "conn2", "oracle",
                        "jdbc:oracle:thin:@localhost:1521/xe", "DDL2", "hint2", null,
                        null, false, Instant.now(), Instant.now()));

        mockMvc.perform(put("/qa/profiles/p1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dbType").value("oracle"));
    }

    @Test
    @DisplayName("DELETE /qa/profiles/{name} — 刪除 profile")
    void shouldDeleteProfile() throws Exception {
        doNothing().when(profileService).delete("p1");

        mockMvc.perform(delete("/qa/profiles/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
