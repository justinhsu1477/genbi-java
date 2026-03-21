package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Agent 子任務的 SQL 搜索結果
 * Agent sub-task SQL search result
 *
 * @param subTaskQuery    子任務查詢 sub-task query text
 * @param sqlSearchResult 該子任務的 SQL 結果 SQL result for this sub-task
 */
public record TaskSqlSearchResult(
        @JsonProperty("sub_task_query") String subTaskQuery,
        @JsonProperty("sql_search_result") SqlSearchResult sqlSearchResult
) {}
