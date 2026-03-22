package com.lndata.genbi.controller;

import com.lndata.genbi.dto.ApiResponse;
import com.lndata.genbi.dto.ProfileRequest;
import com.lndata.genbi.dto.ProfileResponse;
import com.lndata.genbi.service.DbProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Profile 管理 Controller — DB 連線 + DDL + Hints + RLS 設定
 */
@RestController
@RequestMapping("/qa/profiles")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "資料庫 Profile 管理 API")
public class ProfileController {

    private final DbProfileService profileService;

    @GetMapping
    @Operation(summary = "取得所有 Profile")
    public ApiResponse<List<ProfileResponse>> listProfiles() {
        return ApiResponse.ok(profileService.getAllProfiles());
    }

    @GetMapping("/{profileName}")
    @Operation(summary = "依名稱取得 Profile")
    public ApiResponse<ProfileResponse> getProfile(@PathVariable String profileName) {
        return ApiResponse.ok(profileService.getByName(profileName));
    }

    @PostMapping
    @Operation(summary = "建立 Profile")
    public ApiResponse<ProfileResponse> createProfile(@Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(profileService.create(request));
    }

    @PutMapping("/{profileName}")
    @Operation(summary = "更新 Profile")
    public ApiResponse<ProfileResponse> updateProfile(@PathVariable String profileName,
                                                       @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(profileService.update(profileName, request));
    }

    @DeleteMapping("/{profileName}")
    @Operation(summary = "刪除 Profile")
    public ApiResponse<Void> deleteProfile(@PathVariable String profileName) {
        profileService.delete(profileName);
        return ApiResponse.ok();
    }
}
