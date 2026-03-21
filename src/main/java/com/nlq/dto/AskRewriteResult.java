package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 查詢改寫結果 (LLM 反問用戶)
 * Ask-in-reply result — LLM asks a follow-up question
 *
 * @param queryRewrite LLM 改寫/反問的內容 rewritten query or follow-up question
 */
public record AskRewriteResult(
        @JsonProperty("query_rewrite") String queryRewrite
) {
    public static AskRewriteResult empty() {
        return new AskRewriteResult("");
    }
}
