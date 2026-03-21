package com.nlq.service;

import com.nlq.dto.FeedbackRequest;
import com.nlq.entity.UserFeedback;
import com.nlq.repository.UserFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用戶回饋服務 — 處理 upvote / downvote
 * User feedback service — handles upvote / downvote
 *
 * 對應 Python: /qa/user_feedback endpoint
 * upvote 會加入 RAG 做為範例（未來實作）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final UserFeedbackRepository feedbackRepository;

    /**
     * 儲存回饋
     * Save user feedback
     */
    public void saveFeedback(FeedbackRequest request) {
        log.info("[Feedback] saveFeedback: session={}, type={}, user={}",
                request.sessionId(), request.feedbackType(), request.userId());

        feedbackRepository.save(UserFeedback.builder()
                .sessionId(request.sessionId())
                .userId(request.userId())
                .messageId(request.messageId())
                .feedbackType(request.feedbackType())
                .query(request.query())
                .sqlText(request.sqlText())
                .comment(request.comment())
                .build());

        // TODO: upvote 時加入 OpenSearch RAG 索引
        // TODO: on upvote, add to OpenSearch RAG index
        if ("upvote".equals(request.feedbackType())) {
            log.info("[Feedback] Upvote recorded — will be added to RAG in future implementation");
        }
    }
}
