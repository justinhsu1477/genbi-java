package com.lndata.genbi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * 實體消歧請求（同名實體讓用戶選擇）
 *
 * @param entitySelectInfo 同名實體資訊
 * @param entityRetrieval  檢索到的所有實體
 */
public record AskEntitySelect(
        @JsonProperty("entity_select_info") Map<String, Object> entitySelectInfo,
        @JsonProperty("entity_retrieval") List<Object> entityRetrieval
) {
    public static AskEntitySelect empty() {
        return new AskEntitySelect(Map.of(), List.of());
    }
}
