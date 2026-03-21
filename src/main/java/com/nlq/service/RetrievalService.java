package com.nlq.service;

import java.util.List;
import java.util.Map;

/**
 * 向量檢索服務介面 - OpenSearch RAG 檢索
 * Retrieval service interface — OpenSearch vector search (RAG)
 *
 * 對應 Python 的 utils/opensearch.py + utils/text_search.py
 * Maps to Python's utils/opensearch.py + utils/text_search.py
 */
public interface RetrievalService {

    /**
     * 實體檢索 (NER 槽位 -> OpenSearch 向量搜索)
     * Entity retrieval — search entities by NER slots
     *
     * @param entitySlots NER 提取的槽位 extracted NER slots
     * @param profileName profile 名稱 profile name
     * @return 檢索到的實體列表 list of entity results with scores
     */
    List<Map<String, Object>> entityRetrieveSearch(List<Object> entitySlots, String profileName);

    /**
     * QA 範例檢索 (找相似問答對)
     * QA retrieval — find similar Q&A pairs
     *
     * @param query       查詢 user query
     * @param profileName profile 名稱 profile name
     * @return 相似 QA 列表 list of similar Q&A examples
     */
    List<Object> qaRetrieveSearch(String query, String profileName);

    /**
     * Agent 範例檢索
     * Agent example retrieval
     */
    List<Object> agentRetrieveSearch(String query, String profileName);

    /**
     * Agent 子任務 SQL 生成 (含檢索)
     * Agent sub-task SQL generation with retrieval
     *
     * @return 各子任務結果 list of sub-task results [{query, sql, response}, ...]
     */
    List<Map<String, Object>> agentTextSearch(String query, String modelId,
                                               Map<String, Object> databaseProfile,
                                               List<Object> entitySlots, String profileName,
                                               boolean useRag, Map<String, Object> taskSplit);
}
