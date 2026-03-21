package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 圖表資料
 *
 * @param chartType 圖表類型 (bar, line, pie, etc.)
 * @param chartData 圖表數據
 */
public record ChartEntity(
        @JsonProperty("chart_type") String chartType,
        @JsonProperty("chart_data") List<Object> chartData
) {
    public ChartEntity {
        if (chartType == null) chartType = "";
        if (chartData == null) chartData = List.of();
    }
}
