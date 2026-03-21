package com.nlq.dto;

import com.nlq.enums.QueryState;
import java.util.List;
import java.util.Map;

/**
 * 處理上下文 - 攜帶整個查詢過程所需的所有參數
 * Processing context — carries all parameters needed throughout the query flow
 *
 * @param searchBox              原始查詢 original user query
 * @param queryRewrite           改寫後的查詢 rewritten query
 * @param sessionId              會話 ID session id
 * @param userId                 用戶 ID user id
 * @param username               用戶名稱 username
 * @param selectedProfile        選擇的 profile database profile name
 * @param databaseProfile        profile 詳細資訊 database profile details (tables, hints, prompts, db_url, db_type)
 * @param modelType              LLM 模型 ID model identifier
 * @param useRagFlag             是否用 RAG whether to use RAG
 * @param intentNerRecognitionFlag 是否做意圖/NER whether to do intent + NER
 * @param agentCotFlag           是否啟用 Agent whether to enable agent CoT
 * @param explainGenProcessFlag  是否解釋 SQL 生成 whether to explain generation
 * @param visualizeResultsFlag   是否可視化 whether to visualize
 * @param dataWithAnalyse        是否附帶分析 whether to include insights
 * @param genSuggestedQuestionFlag 是否生成建議問題 whether to suggest follow-ups
 * @param autoCorrection         是否自動修正 SQL whether to auto-correct failed SQL
 * @param contextWindow          上下文窗口大小 context window size
 * @param userQueryHistory       用戶歷史查詢 user's recent query history
 * @param previousState          前一狀態 previous state name
 * @param entityRetrieval        實體檢索結果 entity retrieval data
 * @param entityUserSelect       用戶選擇的實體 user-selected entities
 */
public record ProcessingContext(
        String searchBox,
        String queryRewrite,
        String sessionId,
        String userId,
        String username,
        String selectedProfile,
        Map<String, Object> databaseProfile,
        String modelType,
        boolean useRagFlag,
        boolean intentNerRecognitionFlag,
        boolean agentCotFlag,
        boolean explainGenProcessFlag,
        boolean visualizeResultsFlag,
        boolean dataWithAnalyse,
        boolean genSuggestedQuestionFlag,
        boolean autoCorrection,
        int contextWindow,
        List<String> userQueryHistory,
        String previousState,
        List<Object> entityRetrieval,
        Map<String, Object> entityUserSelect
) {
    /**
     * 從 Question 建立 ProcessingContext
     * Build ProcessingContext from a Question DTO
     */
    public static ProcessingContext from(Question q, Map<String, Object> dbProfile, List<String> history) {
        String prevState = "entity_select".equals(q.previousIntent())
                ? QueryState.USER_SELECT_ENTITY.name()
                : QueryState.INITIAL.name();

        return new ProcessingContext(
                q.query(),
                q.queryRewrite(),
                q.sessionId(),
                q.userId(),
                q.username(),
                q.profileName(),
                dbProfile,
                q.bedrockModelId(),
                q.useRagFlag(),
                q.intentNerRecognitionFlag(),
                q.agentCotFlag(),
                q.explainGenProcessFlag(),
                true,  // visualizeResultsFlag
                q.answerWithInsights(),
                q.genSuggestedQuestionFlag(),
                true,  // autoCorrection
                q.contextWindow(),
                history,
                prevState,
                q.entityRetrieval(),
                q.entityUserSelect()
        );
    }
}
