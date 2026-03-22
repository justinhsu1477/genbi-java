package com.lndata.genbi.repository;

import com.lndata.genbi.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用戶回饋 Repository
 */
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    List<UserFeedback> findBySessionId(String sessionId);

    // 查所有 upvote（可用於 RAG 訓練資料匯出）
    List<UserFeedback> findByFeedbackType(String feedbackType);
}
