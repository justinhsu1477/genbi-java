package com.lndata.genbi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * 實體 (NER) 範例新增請求 — 對應 Python VectorStore.add_entity_sample()
 *
 * @param profileName     profile 名稱
 * @param entity          實體名稱 (如: "台積電")
 * @param comment         說明 (如: "客戶名稱")
 * @param entityType      類型: metrics 或 dimension
 * @param entityTableInfo 維度關聯表資訊 [{table_name, column_name, value}]
 */
public record EntitySampleRequest(
        @NotBlank @JsonProperty("profile_name") String profileName,
        @NotBlank String entity,
        String comment,
        @JsonProperty("entity_type") String entityType,
        @JsonProperty("entity_table_info") List<Map<String, String>> entityTableInfo
) {
    public EntitySampleRequest {
        if (entityType == null) entityType = "metrics";
        if (entityTableInfo == null) entityTableInfo = List.of();
        if (comment == null) comment = "";
    }
}
