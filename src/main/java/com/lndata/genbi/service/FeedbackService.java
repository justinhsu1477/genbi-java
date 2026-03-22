package com.lndata.genbi.service;

import com.lndata.genbi.model.dto.FeedbackRequest;
import com.lndata.genbi.model.entity.UserFeedback;
import com.lndata.genbi.repository.UserFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用戶回饋服務 — 處理 upvote / downvote，upvote 會加入 RAG 做為範例（未來實作）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final UserFeedbackRepository feedbackRepository;

    /** 儲存回饋 */
    public void saveFeedback(FeedbackRequest request) {
        log.info("[Feedback] saveFeedback: session={}, type={}, user={}",
                request.sessionId(), request.feedbackType(), request.userId());

        UserFeedback feedback = new UserFeedback();
        feedback.setSessionId(request.sessionId());
        feedback.setUserId(request.userId());
        feedback.setMessageId(request.messageId());
        feedback.setFeedbackType(request.feedbackType());
        feedback.setQuery(request.query());
        feedback.setSqlText(request.sqlText());
        feedback.setComment(request.comment());
        feedbackRepository.save(feedback);

        // TODO: upvote 時加入 OpenSearch RAG 索引
        if ("upvote".equals(request.feedbackType())) {
            log.info("[Feedback] Upvote recorded — will be added to RAG in future implementation");
        }
    }
}
