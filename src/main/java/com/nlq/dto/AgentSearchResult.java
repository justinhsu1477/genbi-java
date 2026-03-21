package com.nlq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Agent 搜索結果 (多子任務匯總)
 * Agent search result — aggregated from multiple sub-tasks
 *
 * @param agentSqlSearchResult 各子任務結果列表 list of sub-task results
 * @param agentSummary         Agent 匯總分析 summary of all sub-task data
 */
public record AgentSearchResult(
        @JsonProperty("agent_sql_search_result") List<TaskSqlSearchResult> agentSqlSearchResult,
        @JsonProperty("agent_summary") String agentSummary
) {
    public static AgentSearchResult empty() {
        return new AgentSearchResult(List.of(), "");
    }
}
