package com.nlq.repository;

import com.nlq.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用戶回饋 Repository
 * User feedback data access
 *
 * 對應 Python: /qa/user_feedback endpoint
 */
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    // 依 session 查回饋
    List<UserFeedback> findBySessionId(String sessionId);

    // 查所有 upvote（可用於 RAG 訓練資料匯出）
    // Find all upvotes (for RAG training data export)
    List<UserFeedback> findByFeedbackType(String feedbackType);
}
