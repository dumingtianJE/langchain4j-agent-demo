package com.yourcompany.langchain4j.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习经验数据库实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "learning_experiences")
public class LearningExperienceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(length = 2000)
    private String query;
    
    @Column(length = 5000)
    private String response;
    
    @Column(length = 1000)
    private String userFeedback;
    
    @Column(name = "feedback_score")
    private Integer feedbackScore;
    
    @ElementCollection
    private String[] usedSkills;
    
    @ElementCollection
    private String[] retrievedKnowledge;
    
    @ElementCollection
    private String[] usedTools;
    
    @Column(name = "tokens_used")
    private Long tokensUsed;
    
    @Column(length = 2000)
    private String learnedImprovement;
    
    private String category;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "is_reviewed")
    private Boolean isReviewed;
}
