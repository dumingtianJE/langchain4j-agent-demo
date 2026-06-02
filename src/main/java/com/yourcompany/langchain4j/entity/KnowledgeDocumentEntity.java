package com.yourcompany.langchain4j.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文档数据库实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocumentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 10000)
    private String content;
    
    @Column(nullable = false)
    private String category;
    
    @ElementCollection
    private List<String> tags;
    
    private String source;
    
    @Column(name = "embedding_id")
    private String embeddingId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "access_count")
    private Integer accessCount;
}
