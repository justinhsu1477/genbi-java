package com.lndata.genbi.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Agent 搜索結果（多子任務匯總）
 *
 * @param agentSqlSearchResult 各子任務結果列表
 * @param agentSummary         匯總分析
 */
public record AgentSearchResult(
        @JsonProperty("agent_sql_search_result") List<TaskSqlSearchResult> agentSqlSearchResult,
        @JsonProperty("agent_summary") String agentSummary
) {
    public static AgentSearchResult empty() {
        return new AgentSearchResult(List.of(), "");
    }
}
