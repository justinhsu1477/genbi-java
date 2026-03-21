package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 知識搜索結果（LLM 直接回答，不走 SQL）
 *
 * @param knowledgeResponse LLM 回答內容
 */
public record KnowledgeSearchResult(
        @JsonProperty("knowledge_response") String knowledgeResponse
) {
    public static KnowledgeSearchResult empty() {
        return new KnowledgeSearchResult("");
    }
}
