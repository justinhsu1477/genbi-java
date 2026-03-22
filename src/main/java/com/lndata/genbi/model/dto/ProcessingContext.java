package com.lndata.genbi.model.dto;

import com.lndata.genbi.model.constant.QueryState;
import java.util.List;
import java.util.Map;

/**
 * 處理上下文 — 攜帶整個查詢流程所需的所有參數
 *
 * @param searchBox              原始查詢
 * @param queryRewrite           改寫後的查詢
 * @param sessionId              會話 ID
 * @param userId                 用戶 ID
 * @param username               用戶名稱
 * @param selectedProfile        選擇的 profile
 * @param databaseProfile        profile 詳細資訊 (tables, hints, prompts, db_url, db_type)
 * @param modelType              LLM 模型 ID
 * @param useRagFlag             是否用 RAG
 * @param intentNerRecognitionFlag 是否做意圖/NER
 * @param agentCotFlag           是否啟用 Agent
 * @param explainGenProcessFlag  是否解釋 SQL 生成過程
 * @param visualizeResultsFlag   是否做數據可視化
 * @param dataWithAnalyse        是否附帶數據分析
 * @param genSuggestedQuestionFlag 是否生成建議問題
 * @param autoCorrection         是否自動修正 SQL
 * @param contextWindow          上下文窗口大小
 * @param userQueryHistory       用戶歷史查詢
 * @param previousState          前一狀態
 * @param entityRetrieval        實體檢索結果
 * @param entityUserSelect       用戶選擇的實體
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
    /** 從 Question 建立 ProcessingContext */
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
