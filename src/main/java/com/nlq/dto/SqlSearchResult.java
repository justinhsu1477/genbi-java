package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * SQL 搜索結果
 * SQL search result — contains generated SQL, execution data, insights
 *
 * @param sql           生成的 SQL the generated SQL
 * @param sqlData       SQL 執行結果 query result rows
 * @param dataShowType  展示方式 display type (table/chart)
 * @param sqlGenProcess SQL 生成過程說明 explanation of SQL generation
 * @param dataAnalyse   數據分析結論 data analysis insights
 * @param sqlDataChart  圖表數據 chart entities
 */
public record SqlSearchResult(
        String sql,
        @JsonProperty("sql_data") List<Object> sqlData,
        @JsonProperty("data_show_type") String dataShowType,
        @JsonProperty("sql_gen_process") String sqlGenProcess,
        @JsonProperty("data_analyse") String dataAnalyse,
        @JsonProperty("sql_data_chart") List<ChartEntity> sqlDataChart
) {
    public static SqlSearchResult empty() {
        return new SqlSearchResult("", List.of(), "table", "", "", List.of());
    }
}
