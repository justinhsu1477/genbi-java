package com.lndata.genbi.service;

import com.lndata.genbi.dto.*;

import java.util.List;

/**
 * 範例資料管理介面 — OpenSearch 索引中的 SQL/Entity/Agent 範例 CRUD
 *
 * 對應 Python: VectorStore (vector_store.py)
 */
public interface SampleManagementService {

    // --- SQL 範例 (sql_index) ---

    /** 查詢指定 profile 的所有 SQL 範例 */
    List<SampleResponse> getAllSqlSamples(String profileName);

    /** 新增 SQL 範例（重複會先刪除舊的） */
    void addSqlSample(SqlSampleRequest request);

    /** 刪除指定 SQL 範例 */
    void deleteSqlSample(String profileName, String docId);

    // --- Entity 範例 (ner_index) ---

    /** 查詢指定 profile 的所有 Entity 範例 */
    List<SampleResponse> getAllEntitySamples(String profileName);

    /** 新增 Entity 範例 */
    void addEntitySample(EntitySampleRequest request);

    /** 刪除指定 Entity 範例 */
    void deleteEntitySample(String profileName, String docId);

    // --- Agent COT 範例 (agent_index) ---

    /** 查詢指定 profile 的所有 Agent COT 範例 */
    List<SampleResponse> getAllAgentCotSamples(String profileName);

    /** 新增 Agent COT 範例 */
    void addAgentCotSample(AgentCotSampleRequest request);

    /** 刪除指定 Agent COT 範例 */
    void deleteAgentCotSample(String profileName, String docId);
}
