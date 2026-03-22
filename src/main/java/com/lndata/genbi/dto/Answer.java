package com.lndata.genbi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 最終回答 — 包含所有查詢結果
 *
 * 狀態機中需要不斷更新，所以用 mutable class + @Data；sub-results 用 record。
 */
@Data
public class Answer {
    private String query = "";

    @JsonProperty("query_rewrite")
    private String queryRewrite = "";

    @JsonProperty("query_intent")
    private String queryIntent = "";

    @JsonProperty("knowledge_search_result")
    private KnowledgeSearchResult knowledgeSearchResult = KnowledgeSearchResult.empty();

    @JsonProperty("sql_search_result")
    private SqlSearchResult sqlSearchResult = SqlSearchResult.empty();

    @JsonProperty("agent_search_result")
    private AgentSearchResult agentSearchResult = AgentSearchResult.empty();

    @JsonProperty("ask_rewrite_result")
    private AskRewriteResult askRewriteResult = AskRewriteResult.empty();

    @JsonProperty("suggested_question")
    private List<String> suggestedQuestion = List.of();

    @JsonProperty("ask_entity_select")
    private AskEntitySelect askEntitySelect = AskEntitySelect.empty();

    @JsonProperty("error_log")
    private Map<String, Object> errorLog = new HashMap<>();
}
