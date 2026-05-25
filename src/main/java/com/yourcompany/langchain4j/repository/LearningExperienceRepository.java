package com.yourcompany.langchain4j.repository;

import com.yourcompany.langchain4j.entity.LearningExperienceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习经验数据访问层
 */
@Repository
public interface LearningExperienceRepository extends JpaRepository<LearningExperienceEntity, String> {
    
    /**
     * 根据反馈分数范围查询
     */
    List<LearningExperienceEntity> findByFeedbackScoreBetween(int minScore, int maxScore);
    
    /**
     * 根据分类查询
     */
    List<LearningExperienceEntity> findByCategory(String category);
    
    /**
     * 查询最近的体验
     */
    List<LearningExperienceEntity> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * 查询未审核的体验
     */
    List<LearningExperienceEntity> findByIsReviewedFalse();
    
    /**
     * 统计平均反馈分数
     */
    @Query("SELECT AVG(e.feedbackScore) FROM LearningExperienceEntity e WHERE e.feedbackScore IS NOT NULL")
    Double calculateAverageFeedbackScore();
    
    /**
     * 统计指定时间范围内的经验数量
     */
    @Query("SELECT COUNT(e) FROM LearningExperienceEntity e WHERE e.createdAt >= :startTime")
    Long countByCreatedAtAfter(@Param("startTime") LocalDateTime startTime);
}
