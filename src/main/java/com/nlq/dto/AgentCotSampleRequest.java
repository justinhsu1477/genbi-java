package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Agent COT 範例新增請求 — 對應 Python VectorStore.add_agent_cot_sample()
 *
 * @param profileName profile 名稱
 * @param query       複雜查詢
 * @param comment     Chain-of-Thought 拆解步驟
 */
public record AgentCotSampleRequest(
        @NotBlank @JsonProperty("profile_name") String profileName,
        @NotBlank String query,
        @NotBlank String comment
) {}
