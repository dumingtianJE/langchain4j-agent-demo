package com.yourcompany.langchain4j.skill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 技能定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    
    /**
     * 技能唯一标识
     */
    private String id;
    
    /**
     * 技能名称
     */
    private String name;
    
    /**
     * 技能描述
     */
    private String description;
    
    /**
     * 技能分类（编程语言、框架、工具、设计模式等）
     */
    private String category;
    
    /**
     * 技能详细内容（Prompt 模板或使用指南）
     */
    private String content;
    
    /**
     * 关联的关键词（用于检索）
     */
    private List<String> keywords;
    
    /**
     * 熟练程度（1-5）
     */
    private Integer proficiencyLevel;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 使用次数统计
     */
    private Integer usageCount;
}
