package com.yourcompany.langchain4j.repository;

import com.yourcompany.langchain4j.entity.KnowledgeDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识文档数据访问层
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, String> {
    
    /**
     * 根据分类查询文档
     */
    List<KnowledgeDocumentEntity> findByCategory(String category);
    
    /**
     * 根据来源查询文档
     */
    List<KnowledgeDocumentEntity> findBySource(String source);
    
    /**
     * 查询最常访问的文档
     */
    @Query("SELECT k FROM KnowledgeDocumentEntity k ORDER BY k.accessCount DESC")
    List<KnowledgeDocumentEntity> findMostAccessed(int limit);
}
