package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * 實體消歧請求 (同名實體讓用戶選擇)
 * Entity disambiguation — ask user to select from same-name entities
 *
 * @param entitySelectInfo 同名實體資訊 same-name entity info map
 * @param entityRetrieval  檢索到的所有實體 all retrieved entities
 */
public record AskEntitySelect(
        @JsonProperty("entity_select_info") Map<String, Object> entitySelectInfo,
        @JsonProperty("entity_retrieval") List<Object> entityRetrieval
) {
    public static AskEntitySelect empty() {
        return new AskEntitySelect(Map.of(), List.of());
    }
}
