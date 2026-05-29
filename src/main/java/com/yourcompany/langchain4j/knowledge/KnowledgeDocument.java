package com.yourcompany.langchain4j.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    
    /**
     * 文档唯一标识
     */
    private String id;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 文档分类（技术文档、API文档、最佳实践、项目规范等）
     */
    private String category;
    
    /**
     * 标签（用于检索）
     */
    private String[] tags;
    
    /**
     * 来源（文件路径、URL、手动输入等）
     */
    private String source;
    
    /**
     * Embedding ID（向量存储中的 ID）
     */
    private String embeddingId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;
    
    /**
     * 访问次数
     */
    private Integer accessCount;
    
    /**
     * 相关度评分（检索时使用）
     */
    private transient Double relevanceScore;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 兼容旧版本11参数构造函数
     */
    public KnowledgeDocument(String id, String title, String content, String category, 
                            String[] tags, String source, String embeddingId,
                            LocalDateTime createdAt, LocalDateTime lastAccessedAt, 
                            Integer accessCount, Double relevanceScore) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.tags = tags;
        this.source = source;
        this.embeddingId = embeddingId;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
        this.accessCount = accessCount;
        this.relevanceScore = relevanceScore;
        this.updatedAt = null;
    }
}
