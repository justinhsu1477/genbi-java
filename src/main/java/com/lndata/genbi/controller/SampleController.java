package com.lndata.genbi.controller;

import com.lndata.genbi.model.dto.*;
import com.lndata.genbi.model.response.BaseListResponse;
import com.lndata.genbi.model.response.BaseRestResponse;
import com.lndata.genbi.service.SampleManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public BaseListResponse<SampleResponse> getSqlSamples(@RequestParam("profile_name") String profileName) {
        return BaseListResponse.success("OK", sampleManagementService.getAllSqlSamples(profileName));
    }

    /** 新增 SQL 範例 */
    @PostMapping("/sql")
    public BaseRestResponse addSqlSample(@Valid @RequestBody SqlSampleRequest request) {
        sampleManagementService.addSqlSample(request);
        return BaseRestResponse.success("OK");
    }

    /** 刪除 SQL 範例 */
    @DeleteMapping("/sql/{docId}")
    public BaseRestResponse deleteSqlSample(@RequestParam("profile_name") String profileName,
                                              @PathVariable String docId) {
        sampleManagementService.deleteSqlSample(profileName, docId);
        return BaseRestResponse.success("OK");
    }

    // =====================================================
    // Entity 範例 (ner_index)
    // =====================================================

    /** 取得指定 profile 的所有 Entity 範例 */
    @GetMapping("/entities")
    public BaseListResponse<SampleResponse> getEntitySamples(@RequestParam("profile_name") String profileName) {
        return BaseListResponse.success("OK", sampleManagementService.getAllEntitySamples(profileName));
    }

    /** 新增 Entity 範例 */
    @PostMapping("/entities")
    public BaseRestResponse addEntitySample(@Valid @RequestBody EntitySampleRequest request) {
        sampleManagementService.addEntitySample(request);
        return BaseRestResponse.success("OK");
    }

    /** 刪除 Entity 範例 */
    @DeleteMapping("/entities/{docId}")
    public BaseRestResponse deleteEntitySample(@RequestParam("profile_name") String profileName,
                                                 @PathVariable String docId) {
        sampleManagementService.deleteEntitySample(profileName, docId);
        return BaseRestResponse.success("OK");
    }

    // =====================================================
    // Agent COT 範例 (agent_index)
    // =====================================================

    /** 取得指定 profile 的所有 Agent COT 範例 */
    @GetMapping("/agents")
    public BaseListResponse<SampleResponse> getAgentCotSamples(@RequestParam("profile_name") String profileName) {
        return BaseListResponse.success("OK", sampleManagementService.getAllAgentCotSamples(profileName));
    }

    /** 新增 Agent COT 範例 */
    @PostMapping("/agents")
    public BaseRestResponse addAgentCotSample(@Valid @RequestBody AgentCotSampleRequest request) {
        sampleManagementService.addAgentCotSample(request);
        return BaseRestResponse.success("OK");
    }

    /** 刪除 Agent COT 範例 */
    @DeleteMapping("/agents/{docId}")
    public BaseRestResponse deleteAgentCotSample(@RequestParam("profile_name") String profileName,
                                                    @PathVariable String docId) {
        sampleManagementService.deleteAgentCotSample(profileName, docId);
        return BaseRestResponse.success("OK");
    }
}
