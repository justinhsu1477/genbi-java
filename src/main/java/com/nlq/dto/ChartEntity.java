package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 圖表資料
 * Chart data for visualization
 *
 * @param chartType 圖表類型 chart type (bar, line, pie, etc.)
 * @param chartData 圖表數據 chart data rows
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
