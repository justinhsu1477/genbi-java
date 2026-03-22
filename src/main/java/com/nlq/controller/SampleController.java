package com.nlq.controller;

import com.nlq.dto.*;
import com.nlq.service.SampleManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 範例資料管理 REST Controller
 *
 * 對應 Python: Streamlit 的 Index/Entity/Agent Management 頁面
 * 提供 SQL 範例、Entity 範例、Agent COT 範例的 CRUD 端點
 */
@RestController
@RequestMapping("/api/v1/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleManagementService sampleManagementService;

    // =====================================================
    // SQL 範例 (sql_index)
    // =====================================================

    /** 取得指定 profile 的所有 SQL 範例 */
    @GetMapping("/sql")
    public ApiResponse<List<SampleResponse>> getSqlSamples(@RequestParam("profile_name") String profileName) {
        return ApiResponse.ok(sampleManagementService.getAllSqlSamples(profileName));
    }

    /** 新增 SQL 範例 */
    @PostMapping("/sql")
    public ApiResponse<Void> addSqlSample(@Valid @RequestBody SqlSampleRequest request) {
        sampleManagementService.addSqlSample(request);
        return ApiResponse.ok(null);
    }

    /** 刪除 SQL 範例 */
    @DeleteMapping("/sql/{docId}")
    public ApiResponse<Void> deleteSqlSample(@RequestParam("profile_name") String profileName,
                                              @PathVariable String docId) {
        sampleManagementService.deleteSqlSample(profileName, docId);
        return ApiResponse.ok(null);
    }

    // =====================================================
    // Entity 範例 (ner_index)
    // =====================================================

    /** 取得指定 profile 的所有 Entity 範例 */
    @GetMapping("/entities")
    public ApiResponse<List<SampleResponse>> getEntitySamples(@RequestParam("profile_name") String profileName) {
        return ApiResponse.ok(sampleManagementService.getAllEntitySamples(profileName));
    }

    /** 新增 Entity 範例 */
    @PostMapping("/entities")
    public ApiResponse<Void> addEntitySample(@Valid @RequestBody EntitySampleRequest request) {
        sampleManagementService.addEntitySample(request);
        return ApiResponse.ok(null);
    }

    /** 刪除 Entity 範例 */
    @DeleteMapping("/entities/{docId}")
    public ApiResponse<Void> deleteEntitySample(@RequestParam("profile_name") String profileName,
                                                 @PathVariable String docId) {
        sampleManagementService.deleteEntitySample(profileName, docId);
        return ApiResponse.ok(null);
    }

    // =====================================================
    // Agent COT 範例 (agent_index)
    // =====================================================

    /** 取得指定 profile 的所有 Agent COT 範例 */
    @GetMapping("/agents")
    public ApiResponse<List<SampleResponse>> getAgentCotSamples(@RequestParam("profile_name") String profileName) {
        return ApiResponse.ok(sampleManagementService.getAllAgentCotSamples(profileName));
    }

    /** 新增 Agent COT 範例 */
    @PostMapping("/agents")
    public ApiResponse<Void> addAgentCotSample(@Valid @RequestBody AgentCotSampleRequest request) {
        sampleManagementService.addAgentCotSample(request);
        return ApiResponse.ok(null);
    }

    /** 刪除 Agent COT 範例 */
    @DeleteMapping("/agents/{docId}")
    public ApiResponse<Void> deleteAgentCotSample(@RequestParam("profile_name") String profileName,
                                                    @PathVariable String docId) {
        sampleManagementService.deleteAgentCotSample(profileName, docId);
        return ApiResponse.ok(null);
    }
}
