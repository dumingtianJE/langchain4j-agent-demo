package com.yourcompany.langchain4j.learning;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习经验记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningExperience {
    
    /**
     * 经验唯一标识
     */
    private String id;
    
    /**
     * 用户查询/问题
     */
    private String query;
    
    /**
     * Agent 的响应
     */
    private String response;
    
    /**
     * 用户反馈（1-5 分，或负面反馈文本）
     */
    private String userFeedback;
    
    /**
     * 反馈分数（1-5）
     */
    private Integer feedbackScore;
    
    /**
     * 使用的技能 ID 列表
     */
    private String[] usedSkills;
    
    /**
     * 检索到的知识文档 ID 列表
     */
    private String[] retrievedKnowledge;
    
    /**
     * 使用的工具列表
     */
    private String[] usedTools;
    
    /**
     * Token 使用统计
     */
    private Long tokensUsed;
    
    /**
     * 学习到的改进点
     */
    private String learnedImprovement;
    
    /**
     * 经验分类
     */
    private String category;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 是否已审核
     */
    private Boolean isReviewed;
}
