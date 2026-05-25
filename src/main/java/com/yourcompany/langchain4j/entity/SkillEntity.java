package com.yourcompany.langchain4j.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 技能数据库实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skills")
public class SkillEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private String category;
    
    @Column(length = 5000)
    private String content;
    
    @ElementCollection
    private java.util.List<String> keywords;
    
    @Column(name = "proficiency_level")
    private Integer proficiencyLevel;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "usage_count")
    private Integer usageCount;
}
