package com.nlq.statemachine;

import com.nlq.dto.*;
import com.nlq.enums.QueryState;
import com.nlq.service.DatabaseService;
import com.nlq.service.LlmService;
import com.nlq.service.RetrievalService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 查詢狀態機 — 核心流程控制
 *
 * 流程概覽:
 * INITIAL -> (Query Rewrite) -> INTENT_RECOGNITION
 *   ├── reject_search   -> REJECT_INTENT -> COMPLETE
 *   ├── knowledge_search -> KNOWLEDGE_SEARCH -> COMPLETE
 *   ├── agent_search    -> AGENT_TASK -> AGENT_SEARCH -> AGENT_DATA_SUMMARY -> COMPLETE
 *   └── normal_search   -> ENTITY_RETRIEVAL -> QA_RETRIEVAL -> SQL_GENERATION -> EXECUTE_QUERY
 *                            ├── (同名實體) -> ASK_ENTITY_SELECT -> COMPLETE
 *                            └── -> ANALYZE_DATA -> COMPLETE
 *
 * 完成後: DATA_VISUALIZATION, SUGGEST_QUESTION, ADD_LOG
 */
@Slf4j
public class QueryStateMachine {

    // 當前狀態
    private QueryState state;
    private final QueryState previousState;

    // 上下文和結果
    private final ProcessingContext context;
    private final Answer answer = new Answer();

    // 外部服務
    private final LlmService llmService;
    private final DatabaseService databaseService;
    private final RetrievalService retrievalService;

    // 內部狀態
    private boolean searchIntentFlag = false;
    private boolean rejectIntentFlag = false;
    private boolean agentIntentFlag = false;
    private boolean knowledgeSearchFlag = false;

    // 中間結果
    private Map<String, Object> intentResponse = Map.of();
    private List<Object> entitySlot = List.of();
    private List<Map<String, Object>> normalSearchEntitySlot = List.of();
    private List<Object> normalSearchQaRetrieval = List.of();
    private Map<String, Object> agentTaskSplit = Map.of();
    private List<Map<String, Object>> agentSearchResult = List.of();

    // SQL 生成中間結果
    private final Map<String, Object> intentSearchResult = new HashMap<>();

    public QueryStateMachine(ProcessingContext context,
                             LlmService llmService,
                             DatabaseService databaseService,
                             RetrievalService retrievalService) {
        this.context = context;
        this.llmService = llmService;
        this.databaseService = databaseService;
        this.retrievalService = retrievalService;

        // 決定初始狀態
        this.previousState = QueryState.USER_SELECT_ENTITY.name().equals(context.previousState())
                ? QueryState.USER_SELECT_ENTITY
                : QueryState.INITIAL;
        this.state = this.previousState == QueryState.USER_SELECT_ENTITY
                ? QueryState.USER_SELECT_ENTITY
                : QueryState.INITIAL;
    }

    // --- 公開方法 ---

    public QueryState getState() { return state; }
    public Answer getAnswer() { return answer; }
    public boolean isTerminal() { return state == QueryState.COMPLETE || state == QueryState.ERROR; }
    public boolean isSearchIntent() { return searchIntentFlag; }
    public boolean isAgentIntent() { return agentIntentFlag; }

    public void transition(QueryState newState) {
        log.info("State transition: {} -> {}", state, newState);
        this.state = newState;
    }

    /** 執行當前狀態的 handler */
    public void executeCurrentState() {
        switch (state) {
            case INITIAL -> handleInitial();
            case INTENT_RECOGNITION -> handleIntentRecognition();
            case REJECT_INTENT -> handleRejectIntent();
            case KNOWLEDGE_SEARCH -> handleKnowledgeSearch();
            case ENTITY_RETRIEVAL -> handleEntityRetrieval();
            case QA_RETRIEVAL -> handleQaRetrieval();
            case SQL_GENERATION -> handleSqlGeneration();
            case EXECUTE_QUERY -> handleExecuteQuery();
            case ANALYZE_DATA -> handleAnalyzeData();
            case AGENT_TASK -> handleAgentTask();
            case AGENT_SEARCH -> handleAgentSqlGeneration();
            case AGENT_DATA_SUMMARY -> handleAgentAnalyzeData();
            case ASK_ENTITY_SELECT -> handleEntitySelection();
            case USER_SELECT_ENTITY -> handleUserSelectEntity();
            default -> {
                log.error("Unknown state: {}", state);
                transition(QueryState.ERROR);
            }
        }
    }

    // =====================================================
    // 狀態處理器
    // =====================================================

    /** 初始狀態：設定查詢，嘗試改寫 */
    void handleInitial() {
        try {
            answer.setQuery(context.searchBox());
            answer.setQueryRewrite(context.searchBox());
            answer.setQueryIntent("normal_search");

            if (context.contextWindow() > 0) {
                handleQueryRewrite();
            } else {
                transition(QueryState.INTENT_RECOGNITION);
            }
        } catch (Exception e) {
            log.error("handleInitial error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.INITIAL.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** 查詢改寫：用歷史對話讓 LLM 改寫問題 */
    private void handleQueryRewrite() {
        try {
            Map<String, Object> promptMap = getPromptMap();
            Map<String, Object> result = llmService.getQueryRewrite(
                    context.modelType(), context.searchBox(), promptMap, context.userQueryHistory());

            String intent = (String) result.getOrDefault("intent", "normal");
            String rewrittenQuery = (String) result.getOrDefault("query", context.searchBox());

            if ("ask_in_reply".equals(intent)) {
                // LLM 認為需要反問用戶
                answer.setQuery(context.searchBox());
                answer.setQueryIntent("ask_in_reply");
                answer.setQueryRewrite(rewrittenQuery);
                answer.setAskRewriteResult(new AskRewriteResult(rewrittenQuery));
                transition(QueryState.COMPLETE);
            } else {
                answer.setQueryRewrite(rewrittenQuery);
                answer.setQuery(context.searchBox());
                transition(QueryState.INTENT_RECOGNITION);
            }
        } catch (Exception e) {
            log.error("handleQueryRewrite error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.QUERY_REWRITE.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /**
     * 意圖識別：LLM 判斷查詢類型 + NER 抽取
     *
     * 可能的意圖: normal_search, agent_search, knowledge_search, reject_search
     */
    @SuppressWarnings("unchecked")
    void handleIntentRecognition() {
        try {
            if (context.intentNerRecognitionFlag()) {
                Map<String, Object> promptMap = getPromptMap();
                String queryRewrite = answer.getQueryRewrite();
                intentResponse = llmService.getQueryIntent(context.modelType(), queryRewrite, promptMap);

                String intent = (String) intentResponse.getOrDefault("intent", "normal_search");
                entitySlot = (List<Object>) intentResponse.getOrDefault("slot", List.of());

                processIntentResponse(intent);
            } else {
                searchIntentFlag = true;
            }
            transitionBasedOnIntent();
        } catch (Exception e) {
            log.error("handleIntentRecognition error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.INTENT_RECOGNITION.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    private void processIntentResponse(String intent) {
        switch (intent) {
            case "reject_search" -> {
                rejectIntentFlag = true;
                searchIntentFlag = false;
            }
            case "agent_search" -> {
                agentIntentFlag = true;
                if (context.agentCotFlag()) {
                    searchIntentFlag = false;
                } else {
                    searchIntentFlag = true;
                    agentIntentFlag = false;
                }
            }
            case "knowledge_search" -> {
                knowledgeSearchFlag = true;
                searchIntentFlag = false;
                agentIntentFlag = false;
            }
            default -> searchIntentFlag = true; // normal_search
        }
    }

    private void transitionBasedOnIntent() {
        if (rejectIntentFlag) {
            answer.setQueryIntent("reject_search");
            transition(QueryState.REJECT_INTENT);
        } else if (knowledgeSearchFlag) {
            transition(QueryState.KNOWLEDGE_SEARCH);
        } else if (agentIntentFlag) {
            answer.setQueryIntent("agent_search");
            transition(QueryState.AGENT_TASK);
        } else {
            answer.setQueryIntent("normal_search");
            transition(QueryState.ENTITY_RETRIEVAL);
        }
    }

    /** 拒絕意圖：不回答此查詢 */
    void handleRejectIntent() {
        answer.setQuery(context.searchBox());
        answer.setQueryRewrite(answer.getQueryRewrite());
        answer.setQueryIntent("reject_search");
        transition(QueryState.COMPLETE);
    }

    /** 知識搜索：LLM 直接回答，不生成 SQL */
    void handleKnowledgeSearch() {
        try {
            Map<String, Object> promptMap = getPromptMap();
            String response = llmService.knowledgeSearch(answer.getQueryRewrite(), context.modelType(), promptMap);

            answer.setQuery(context.searchBox());
            answer.setQueryRewrite(answer.getQueryRewrite());
            answer.setQueryIntent("knowledge_search");
            answer.setKnowledgeSearchResult(new KnowledgeSearchResult(response));
            transition(QueryState.COMPLETE);
        } catch (Exception e) {
            log.error("handleKnowledgeSearch error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.KNOWLEDGE_SEARCH.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /**
     * 實體檢索：用 NER 槽位做向量搜索
     * 如果找到同名實體 (score > 0.98)，會轉到 ASK_ENTITY_SELECT 讓用戶選
     */
    @SuppressWarnings("unchecked")
    void handleEntityRetrieval() {
        try {
            if (context.useRagFlag()) {
                normalSearchEntitySlot = retrievalService.entityRetrieveSearch(entitySlot, context.selectedProfile());
            } else {
                normalSearchEntitySlot = List.of();
            }

            // 檢查同名實體
            Map<String, Object> sameNameEntity = new LinkedHashMap<>();
            for (Map<String, Object> entity : normalSearchEntitySlot) {
                Map<String, Object> source = (Map<String, Object>) entity.get("_source");
                double score = ((Number) entity.get("_score")).doubleValue();
                int entityCount = ((Number) source.getOrDefault("entity_count", 1)).intValue();

                if (entityCount > 1 && score > 0.98) {
                    sameNameEntity.put((String) source.get("entity"), source.get("entity_table_info"));
                }
            }

            if (!sameNameEntity.isEmpty() && "normal_search".equals(answer.getQueryIntent())) {
                answer.setAskEntitySelect(new AskEntitySelect(sameNameEntity, List.copyOf(normalSearchEntitySlot)));
                transition(QueryState.ASK_ENTITY_SELECT);
            } else {
                transition(QueryState.QA_RETRIEVAL);
            }
        } catch (Exception e) {
            log.error("handleEntityRetrieval error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.ENTITY_RETRIEVAL.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** QA 檢索：找相似問答對作為 SQL 生成範例 */
    void handleQaRetrieval() {
        try {
            if (context.useRagFlag()) {
                normalSearchQaRetrieval = retrievalService.qaRetrieveSearch(
                        answer.getQueryRewrite(), context.selectedProfile());
            } else {
                normalSearchQaRetrieval = List.of();
            }
            transition(QueryState.SQL_GENERATION);
        } catch (Exception e) {
            log.error("handleQaRetrieval error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.QA_RETRIEVAL.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** SQL 生成：LLM 根據 DDL + 範例 + 查詢生成 SQL */
    void handleSqlGeneration() {
        try {
            String response = generateSql();
            String sql = extractSql(response);

            intentSearchResult.put("sql", sql);
            intentSearchResult.put("response", response);
            intentSearchResult.put("original_sql", sql);

            answer.setSqlSearchResult(new SqlSearchResult(
                    sql.strip(),
                    List.of(),
                    "table",
                    extractSqlExplanation(response),
                    "",
                    List.of()
            ));

            if (context.visualizeResultsFlag()) {
                transition(QueryState.EXECUTE_QUERY);
            } else {
                transition(QueryState.COMPLETE);
            }
        } catch (Exception e) {
            log.error("handleSqlGeneration error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.SQL_GENERATION.name(), e.getMessage());
            transition(QueryState.COMPLETE);
        }
    }

    /** 執行 SQL：對資料庫跑 SQL，失敗可自動修正 */
    @SuppressWarnings("unchecked")
    void handleExecuteQuery() {
        try {
            String sql = (String) intentSearchResult.getOrDefault("sql", "");
            if (sql.isBlank()) {
                answer.getErrorLog().put(QueryState.EXECUTE_QUERY.name(), "SQL is empty");
                transition(QueryState.ERROR);
                return;
            }

            Map<String, Object> result = databaseService.executeSql(context.databaseProfile(), sql);
            int statusCode = (int) result.getOrDefault("statusCode", 500);
            intentSearchResult.put("sql_execute_result", result);

            List<Object> data = (List<Object>) result.getOrDefault("data", List.of());
            answer.setSqlSearchResult(new SqlSearchResult(
                    answer.getSqlSearchResult().sql(),
                    data,
                    "table",
                    answer.getSqlSearchResult().sqlGenProcess(),
                    "",
                    List.of()
            ));

            if (statusCode == 200 && context.dataWithAnalyse()) {
                transition(QueryState.ANALYZE_DATA);
            } else if (statusCode == 200) {
                transition(QueryState.COMPLETE);
            } else if (statusCode == 500 && context.autoCorrection()) {
                // 自動修正：重新生成 SQL
                handleAutoCorrection(result);
            } else {
                answer.getErrorLog().put(QueryState.EXECUTE_QUERY.name(), result.get("errorInfo"));
                transition(QueryState.ERROR);
            }
        } catch (Exception e) {
            log.error("handleExecuteQuery error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.EXECUTE_QUERY.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /**
     * SQL 自動修正 — 把失敗的 SQL + 錯誤訊息餵回 LLM，重新生成一次
     * 對應 Python: _generate_sql_again() + handle_execute_query 中的 auto_correction 邏輯
     */
    @SuppressWarnings("unchecked")
    private void handleAutoCorrection(Map<String, Object> firstResult) {
        log.info("Auto-correcting SQL: original error={}", firstResult.get("errorInfo"));
        try {
            String originalSql = (String) intentSearchResult.getOrDefault("original_sql", "");
            String errorInfo = String.valueOf(firstResult.getOrDefault("errorInfo", "unknown error"));

            // 用 LLM 重新生成 SQL（帶上錯誤資訊）
            String tablesInfo = (String) context.databaseProfile().getOrDefault("tables_info", "");
            String hints = (String) context.databaseProfile().getOrDefault("hints", "");
            String dialect = (String) context.databaseProfile().getOrDefault("db_type", "mysql");
            Map<String, Object> promptMap = getPromptMap();

            String correctedResponse = llmService.textToSqlWithCorrection(
                    tablesInfo, hints, promptMap,
                    answer.getQueryRewrite(), context.modelType(),
                    normalSearchQaRetrieval, List.copyOf(normalSearchEntitySlot), dialect,
                    originalSql, errorInfo);

            String correctedSql = extractSql(correctedResponse);
            log.info("Auto-correction generated new SQL: {}", correctedSql);

            // 執行修正後的 SQL
            Map<String, Object> retryResult = databaseService.executeSql(context.databaseProfile(), correctedSql);
            int retryStatus = (int) retryResult.getOrDefault("statusCode", 500);

            // 更新 answer 的 SQL 結果
            intentSearchResult.put("sql", correctedSql);
            intentSearchResult.put("sql_execute_result", retryResult);

            List<Object> data = (List<Object>) retryResult.getOrDefault("data", List.of());
            answer.setSqlSearchResult(new SqlSearchResult(
                    correctedSql,
                    data,
                    "table",
                    extractSqlExplanation(correctedResponse),
                    "",
                    List.of()
            ));

            if (retryStatus == 200 && context.dataWithAnalyse()) {
                transition(QueryState.ANALYZE_DATA);
            } else if (retryStatus == 200) {
                transition(QueryState.COMPLETE);
            } else {
                // 修正後還是失敗，放棄（只重試一次）
                log.warn("Auto-correction failed again: {}", retryResult.get("errorInfo"));
                answer.getErrorLog().put(QueryState.EXECUTE_QUERY.name(),
                        "Auto-correction failed: " + retryResult.get("errorInfo"));
                transition(QueryState.ERROR);
            }
        } catch (Exception e) {
            log.error("handleAutoCorrection error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.EXECUTE_QUERY.name(),
                    "Auto-correction exception: " + e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** 數據分析：LLM 分析查詢結果，產生 insights */
    void handleAnalyzeData() {
        try {
            Map<String, Object> promptMap = getPromptMap();
            // 將 data 轉成 JSON 字串
            String dataJson = intentSearchResult.get("sql_execute_result") != null
                    ? intentSearchResult.get("sql_execute_result").toString()
                    : "[]";

            String analysisResult = llmService.dataAnalyse(
                    context.modelType(), promptMap, answer.getQueryRewrite(), dataJson, "query");

            answer.setSqlSearchResult(new SqlSearchResult(
                    answer.getSqlSearchResult().sql(),
                    answer.getSqlSearchResult().sqlData(),
                    answer.getSqlSearchResult().dataShowType(),
                    answer.getSqlSearchResult().sqlGenProcess(),
                    analysisResult,
                    answer.getSqlSearchResult().sqlDataChart()
            ));
            transition(QueryState.COMPLETE);
        } catch (Exception e) {
            log.error("handleAnalyzeData error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.ANALYZE_DATA.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** Agent 任務拆解：LLM 把複雜查詢拆成多個子任務 */
    void handleAgentTask() {
        try {
            Map<String, Object> promptMap = getPromptMap();
            String tablesInfo = (String) context.databaseProfile().getOrDefault("tables_info", "");
            List<Object> agentExamples = retrievalService.agentRetrieveSearch(
                    answer.getQueryRewrite(), context.selectedProfile());

            agentTaskSplit = llmService.getAgentCotTask(
                    context.modelType(), promptMap, answer.getQueryRewrite(), tablesInfo, agentExamples);
            transition(QueryState.AGENT_SEARCH);
        } catch (Exception e) {
            log.error("handleAgentTask error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.AGENT_TASK.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** Agent SQL 生成：為每個子任務生成 SQL */
    void handleAgentSqlGeneration() {
        try {
            agentSearchResult = retrievalService.agentTextSearch(
                    answer.getQueryRewrite(), context.modelType(), context.databaseProfile(),
                    entitySlot, context.selectedProfile(), context.useRagFlag(), agentTaskSplit);
            transition(QueryState.AGENT_DATA_SUMMARY);
        } catch (Exception e) {
            log.error("handleAgentSqlGeneration error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.AGENT_SEARCH.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** Agent 數據匯總：執行各子任務 SQL，LLM 匯總分析 */
    @SuppressWarnings("unchecked")
    void handleAgentAnalyzeData() {
        try {
            List<TaskSqlSearchResult> taskResults = new ArrayList<>();

            for (Map<String, Object> task : agentSearchResult) {
                String sql = (String) task.getOrDefault("sql", "");
                Map<String, Object> execResult = databaseService.executeSql(context.databaseProfile(), sql);

                if ((int) execResult.getOrDefault("statusCode", 500) == 200) {
                    List<Object> data = (List<Object>) execResult.getOrDefault("data", List.of());
                    SqlSearchResult subResult = new SqlSearchResult(
                            sql, data, "table",
                            (String) task.getOrDefault("response", ""),
                            "", List.of());
                    taskResults.add(new TaskSqlSearchResult(
                            (String) task.getOrDefault("query", ""), subResult));
                }
            }

            // LLM 匯總
            Map<String, Object> promptMap = getPromptMap();
            String summary = llmService.dataAnalyse(
                    context.modelType(), promptMap, answer.getQueryRewrite(),
                    agentSearchResult.toString(), "agent");

            answer.setAgentSearchResult(new AgentSearchResult(taskResults, summary));
            transition(QueryState.COMPLETE);
        } catch (Exception e) {
            log.error("handleAgentAnalyzeData error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.AGENT_DATA_SUMMARY.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** 實體消歧：同名實體，請用戶選擇 */
    void handleEntitySelection() {
        answer.setQueryIntent("entity_select");
        transition(QueryState.COMPLETE);
    }

    /** 處理用戶選擇的實體，繼續 QA 檢索 */
    void handleUserSelectEntity() {
        try {
            answer.setQuery(context.searchBox());
            answer.setQueryRewrite(context.queryRewrite());
            answer.setQueryIntent("normal_search");
            searchIntentFlag = true;
            // 用戶選了實體，繼續做 QA 檢索
            transition(QueryState.QA_RETRIEVAL);
        } catch (Exception e) {
            log.error("handleUserSelectEntity error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.USER_SELECT_ENTITY.name(), e.getMessage());
            transition(QueryState.ERROR);
        }
    }

    /** 數據可視化：LLM 選擇圖表類型 */
    public void handleDataVisualization() {
        try {
            if (!"normal_search".equals(answer.getQueryIntent())) return;

            Map<String, Object> promptMap = getPromptMap();
            Map<String, Object> vizResult = llmService.dataVisualization(
                    context.modelType(), answer.getQueryRewrite(),
                    answer.getSqlSearchResult().sqlData(), promptMap);

            String showType = (String) vizResult.getOrDefault("showType", "table");
            String chartType = (String) vizResult.getOrDefault("chartType", "-1");

            if (!"-1".equals(chartType)) {
                @SuppressWarnings("unchecked")
                List<Object> chartData = (List<Object>) vizResult.getOrDefault("chartData", List.of());
                ChartEntity chart = new ChartEntity(chartType, chartData);
                answer.setSqlSearchResult(new SqlSearchResult(
                        answer.getSqlSearchResult().sql(),
                        answer.getSqlSearchResult().sqlData(),
                        showType,
                        answer.getSqlSearchResult().sqlGenProcess(),
                        answer.getSqlSearchResult().dataAnalyse(),
                        List.of(chart)
                ));
            }
        } catch (Exception e) {
            log.error("handleDataVisualization error: {}", e.getMessage(), e);
            answer.getErrorLog().put(QueryState.DATA_VISUALIZATION.name(), e.getMessage());
        }
    }

    /** 生成建議問題 */
    public void handleSuggestQuestion() {
        if (!context.genSuggestedQuestionFlag()) return;
        if (!searchIntentFlag && !agentIntentFlag) return;

        try {
            Map<String, Object> promptMap = getPromptMap();
            List<String> suggestions = llmService.generateSuggestedQuestions(
                    promptMap, answer.getQueryRewrite(), context.modelType());
            answer.setSuggestedQuestion(suggestions);
        } catch (Exception e) {
            log.error("handleSuggestQuestion error: {}", e.getMessage(), e);
        }
    }

    // =====================================================
    // 工具方法
    // =====================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPromptMap() {
        return (Map<String, Object>) context.databaseProfile().getOrDefault("prompt_map", Map.of());
    }

    private String generateSql() {
        String tablesInfo = (String) context.databaseProfile().getOrDefault("tables_info", "");
        String hints = (String) context.databaseProfile().getOrDefault("hints", "");
        String dialect = (String) context.databaseProfile().getOrDefault("db_type", "mysql");
        Map<String, Object> promptMap = getPromptMap();

        return llmService.textToSql(tablesInfo, hints, promptMap,
                answer.getQueryRewrite(), context.modelType(),
                normalSearchQaRetrieval, List.copyOf(normalSearchEntitySlot), dialect);
    }

    /** 從 LLM 回應中提取 SQL */
    private String extractSql(String response) {
        // 嘗試找 <sql>...</sql> 標籤
        if (response.contains("<sql>") && response.contains("</sql>")) {
            int start = response.indexOf("<sql>") + 5;
            int end = response.indexOf("</sql>");
            return response.substring(start, end).strip();
        }
        return response.strip();
    }

    private String extractSqlExplanation(String response) {
        // 提取 SQL 前的解釋部分
        if (response.contains("<sql>")) {
            return response.substring(0, response.indexOf("<sql>")).strip();
        }
        return "";
    }
}
