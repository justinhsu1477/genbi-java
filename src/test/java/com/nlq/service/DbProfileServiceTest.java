package com.nlq.service;

import com.nlq.dto.ProfileRequest;
import com.nlq.dto.ProfileResponse;
import com.nlq.entity.DbProfile;
import com.nlq.exception.BusinessException;
import com.nlq.repository.DbProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * DbProfileService 單元測試
 */
@ExtendWith(MockitoExtension.class)
class DbProfileServiceTest {

    @Mock DbProfileRepository profileRepository;
    @Mock RlsService rlsService;
    @InjectMocks DbProfileService profileService;

    private DbProfile buildProfile(String name) {
        DbProfile p = new DbProfile();
        p.setId(1L);
        p.setProfileName(name);
        p.setConnName("conn-" + name);
        p.setDbType("mysql");
        p.setDbUrl("jdbc:mysql://localhost:3306/test");
        p.setTablesInfo("CREATE TABLE t1 (id INT)");
        p.setHints("hint");
        p.setRlsEnabled(false);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private ProfileRequest buildRequest(String name) {
        return new ProfileRequest(name, "conn", "mysql",
                "jdbc:mysql://localhost/db", "user", "pass",
                "CREATE TABLE t1 (id INT)", "hint", "comment",
                false, null);
    }

    @Nested
    @DisplayName("getAllProfiles")
    class ListTests {

        @Test
        @DisplayName("回傳所有 profile")
        void shouldReturnAll() {
            when(profileRepository.findAll()).thenReturn(List.of(buildProfile("p1"), buildProfile("p2")));

            List<ProfileResponse> result = profileService.getAllProfiles();

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("getByName")
    class GetByNameTests {

        @Test
        @DisplayName("找到 profile")
        void shouldReturnProfile() {
            when(profileRepository.findByProfileName("p1")).thenReturn(Optional.of(buildProfile("p1")));

            ProfileResponse result = profileService.getByName("p1");

            assertEquals("p1", result.profileName());
            assertNull(result.rlsEnabled() ? "should not expose password" : null);
        }

        @Test
        @DisplayName("找不到 → 404")
        void shouldThrow404() {
            when(profileRepository.findByProfileName("bad")).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> profileService.getByName("bad"));
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("正常建立")
        void shouldCreate() {
            when(profileRepository.existsByProfileName("new-profile")).thenReturn(false);
            when(profileRepository.save(any())).thenAnswer(inv -> {
                DbProfile p = inv.getArgument(0);
                p.setId(1L);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                return p;
            });

            ProfileResponse result = profileService.create(buildRequest("new-profile"));

            assertEquals("new-profile", result.profileName());
            verify(profileRepository).save(any(DbProfile.class));
        }

        @Test
        @DisplayName("名稱重複 → 400")
        void shouldThrowWhenDuplicate() {
            when(profileRepository.existsByProfileName("dup")).thenReturn(true);

            assertThrows(BusinessException.class, () -> profileService.create(buildRequest("dup")));
            verify(profileRepository, never()).save(any());
        }

        @Test
        @DisplayName("RLS 啟用但設定不合法 → 400")
        void shouldRejectInvalidRls() {
            when(profileRepository.existsByProfileName("rls-test")).thenReturn(false);
            when(rlsService.validateRlsConfig("bad yaml")).thenReturn(false);

            ProfileRequest rlsReq = new ProfileRequest("rls-test", "conn", "mysql",
                    "jdbc:mysql://localhost/db", null, null, null, null, null,
                    true, "bad yaml");

            assertThrows(BusinessException.class, () -> profileService.create(rlsReq));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("正常更新")
        void shouldUpdate() {
            when(profileRepository.findByProfileName("p1")).thenReturn(Optional.of(buildProfile("p1")));
            when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProfileResponse result = profileService.update("p1", buildRequest("p1"));

            assertEquals("p1", result.profileName());
            verify(profileRepository).save(any(DbProfile.class));
        }

        @Test
        @DisplayName("找不到 → 404")
        void shouldThrow404() {
            when(profileRepository.findByProfileName("bad")).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> profileService.update("bad", buildRequest("bad")));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("正常刪除")
        void shouldDelete() {
            when(profileRepository.existsByProfileName("p1")).thenReturn(true);

            profileService.delete("p1");

            verify(profileRepository).deleteByProfileName("p1");
        }

        @Test
        @DisplayName("找不到 → 404")
        void shouldThrow404() {
            when(profileRepository.existsByProfileName("bad")).thenReturn(false);

            assertThrows(BusinessException.class, () -> profileService.delete("bad"));
        }
    }
}
