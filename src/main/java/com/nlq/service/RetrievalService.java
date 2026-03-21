package com.nlq.service;

import java.util.List;
import java.util.Map;

/**
 * 向量檢索服務介面 — OpenSearch RAG 檢索
 */
public interface RetrievalService {

    /**
     * 實體檢索（NER 槽位 -> OpenSearch 向量搜索）
     *
     * @param entitySlots NER 提取的槽位
     * @param profileName profile 名稱
     * @return 檢索到的實體列表（含分數）
     */
    List<Map<String, Object>> entityRetrieveSearch(List<Object> entitySlots, String profileName);

    /**
     * QA 範例檢索（找相似問答對）
     *
     * @param query       用戶查詢
     * @param profileName profile 名稱
     * @return 相似 QA 列表
     */
    List<Object> qaRetrieveSearch(String query, String profileName);

    /** Agent 範例檢索 */
    List<Object> agentRetrieveSearch(String query, String profileName);

    /**
     * Agent 子任務 SQL 生成（含檢索）
     *
     * @return 各子任務結果 [{query, sql, response}, ...]
     */
    List<Map<String, Object>> agentTextSearch(String query, String modelId,
                                               Map<String, Object> databaseProfile,
                                               List<Object> entitySlots, String profileName,
                                               boolean useRag, Map<String, Object> taskSplit);
}
