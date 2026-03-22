package com.lndata.genbi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 查詢改寫結果（LLM 反問用戶）
 *
 * @param queryRewrite LLM 改寫或反問的內容
 */
public record AskRewriteResult(
        @JsonProperty("query_rewrite") String queryRewrite
) {
    public static AskRewriteResult empty() {
        return new AskRewriteResult("");
    }
}
