package com.lndata.genbi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * SQL 範例新增請求 — 對應 Python VectorStore.add_sample()
 *
 * @param profileName profile 名稱
 * @param question    自然語言問題
 * @param sql         對應的 SQL
 */
public record SqlSampleRequest(
        @NotBlank @JsonProperty("profile_name") String profileName,
        @NotBlank String question,
        @NotBlank String sql
) {}
