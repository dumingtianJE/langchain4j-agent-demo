package com.yourcompany.langchain4j.repository;

import com.yourcompany.langchain4j.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 技能数据访问层
 */
@Repository
public interface SkillRepository extends JpaRepository<SkillEntity, String> {
    
    /**
     * 根据分类查询技能
     */
    List<SkillEntity> findByCategory(String category);
    
    /**
     * 根据关键词搜索（名称、描述、关键词列表）
     */
    @Query("SELECT s FROM SkillEntity s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.category) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<SkillEntity> searchByKeyword(@Param("query") String query);
    
    /**
     * 查询所有技能（按熟练度排序）
     */
    @Query("SELECT s FROM SkillEntity s ORDER BY s.proficiencyLevel DESC")
    List<SkillEntity> findAllOrderByProficiency();
}
