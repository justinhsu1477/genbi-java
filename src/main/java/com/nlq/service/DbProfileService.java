package com.nlq.service;

import com.nlq.dto.ProfileRequest;
import com.nlq.dto.ProfileResponse;
import com.nlq.entity.DbProfile;
import com.nlq.exception.BusinessException;
import com.nlq.repository.DbProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Profile 管理服務 — CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbProfileService {

    private final DbProfileRepository profileRepository;
    private final RlsService rlsService;

    /** 取得所有 profile */
    public List<ProfileResponse> getAllProfiles() {
        return profileRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /** 依名稱取得 profile */
    public ProfileResponse getByName(String profileName) {
        return profileRepository.findByProfileName(profileName)
                .map(this::toResponse)
                .orElseThrow(() -> BusinessException.notFound("Profile " + profileName));
    }

    /** 建立 profile */
    @Transactional
    public ProfileResponse create(ProfileRequest request) {
        if (profileRepository.existsByProfileName(request.profileName())) {
            throw BusinessException.badRequest("Profile " + request.profileName() + " already exists");
        }

        // 驗證 RLS 設定
        if (Boolean.TRUE.equals(request.rlsEnabled()) && request.rlsConfig() != null) {
            if (!rlsService.validateRlsConfig(request.rlsConfig())) {
                throw BusinessException.badRequest("Invalid RLS config");
            }
        }

        DbProfile profile = DbProfile.builder()
                .profileName(request.profileName())
                .connName(request.connName())
                .dbType(request.dbType())
                .dbUrl(request.dbUrl())
                .dbUsername(request.dbUsername())
                .dbPassword(request.dbPassword())
                .tablesInfo(request.tablesInfo())
                .hints(request.hints())
                .comments(request.comments())
                .rlsEnabled(Boolean.TRUE.equals(request.rlsEnabled()))
                .rlsConfig(request.rlsConfig())
                .build();

        DbProfile saved = profileRepository.save(profile);
        log.info("[Profile] created: {}", saved.getProfileName());
        return toResponse(saved);
    }

    /** 更新 profile */
    @Transactional
    public ProfileResponse update(String profileName, ProfileRequest request) {
        DbProfile profile = profileRepository.findByProfileName(profileName)
                .orElseThrow(() -> BusinessException.notFound("Profile " + profileName));

        // 驗證 RLS 設定
        if (Boolean.TRUE.equals(request.rlsEnabled()) && request.rlsConfig() != null) {
            if (!rlsService.validateRlsConfig(request.rlsConfig())) {
                throw BusinessException.badRequest("Invalid RLS config");
            }
        }

        profile.updateFrom(request);

        DbProfile saved = profileRepository.save(profile);
        log.info("[Profile] updated: {}", saved.getProfileName());
        return toResponse(saved);
    }

    /** 刪除 profile */
    @Transactional
    public void delete(String profileName) {
        if (!profileRepository.existsByProfileName(profileName)) {
            throw BusinessException.notFound("Profile " + profileName);
        }
        profileRepository.deleteByProfileName(profileName);
        log.info("[Profile] deleted: {}", profileName);
    }

    // --- 轉換方法 ---

    private ProfileResponse toResponse(DbProfile p) {
        return new ProfileResponse(
                p.getId(), p.getProfileName(), p.getConnName(),
                p.getDbType(), p.getDbUrl(),
                p.getTablesInfo(), p.getHints(), p.getComments(),
                p.getRlsEnabled(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
