package com.lndata.genbi.controller;

import com.lndata.genbi.model.dto.ProfileRequest;
import com.lndata.genbi.model.dto.ProfileResponse;
import com.lndata.genbi.model.response.BaseListResponse;
import com.lndata.genbi.model.response.BaseRestResponse;
import com.lndata.genbi.model.response.BaseSingleResponse;
import com.lndata.genbi.service.DbProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public BaseListResponse<ProfileResponse> listProfiles() {
        return BaseListResponse.success("OK", profileService.getAllProfiles());
    }

    @GetMapping("/{profileName}")
    @Operation(summary = "依名稱取得 Profile")
    public BaseSingleResponse<ProfileResponse> getProfile(@PathVariable String profileName) {
        return BaseSingleResponse.success("OK", profileService.getByName(profileName));
    }

    @PostMapping
    @Operation(summary = "建立 Profile")
    public BaseSingleResponse<ProfileResponse> createProfile(@Valid @RequestBody ProfileRequest request) {
        return BaseSingleResponse.success("OK", profileService.create(request));
    }

    @PutMapping("/{profileName}")
    @Operation(summary = "更新 Profile")
    public BaseSingleResponse<ProfileResponse> updateProfile(@PathVariable String profileName,
                                                       @Valid @RequestBody ProfileRequest request) {
        return BaseSingleResponse.success("OK", profileService.update(profileName, request));
    }

    @DeleteMapping("/{profileName}")
    @Operation(summary = "刪除 Profile")
    public BaseRestResponse deleteProfile(@PathVariable String profileName) {
        profileService.delete(profileName);
        return BaseRestResponse.success("OK");
    }
}
