package com.yourcompany.langchain4j.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 多项目知识库管理器
 * 支持项目隔离、跨项目共享、分层检索
 */
@Slf4j
@Service
public class MultiProjectKnowledgeManager {
    
    @Autowired
    private KnowledgeBaseManager knowledgeBaseManager;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    /**
     * 项目知识库映射
     * key: projectId
     * value: 该项目的知识库实例
     */
    private final Map<String, KnowledgeBaseManager> projectKnowledgeBases = new ConcurrentHashMap<>();
    
    /**
     * 项目元数据
     */
    private final Map<String, ProjectMetadata> projectMetadataMap = new ConcurrentHashMap<>();
    
    /**
     * 全局共享知识库（跨项目通用知识）
     */
    private KnowledgeBaseManager globalKnowledgeBase;
    
    @PostConstruct
    public void init() {
        log.info("多项目知识库系统初始化完成");
    }
    
    /**
     * 注册新项目
     */
    public void registerProject(ProjectConfig config) {
        String projectId = config.getProjectId();
        
        if (projectKnowledgeBases.containsKey(projectId)) {
            log.warn("项目已存在: {}", projectId);
            return;
        }
        
        // 创建项目专属知识库
        // 注意：实际项目中需要创建新的 KnowledgeBaseManager 实例
        // 这里简化处理，使用同一个 embeddingStore
        projectKnowledgeBases.put(projectId, knowledgeBaseManager);
        
        // 保存项目元数据
        ProjectMetadata metadata = new ProjectMetadata(config);
        metadata.setCreatedAt(System.currentTimeMillis());
        projectMetadataMap.put(projectId, metadata);
        
        log.info("项目注册成功: {} (技术栈: {}, 领域: {})", 
            projectId, config.getTechStack(), config.getDomain());
    }
    
    /**
     * 向项目知识库添加文档
     */
    public void addProjectDocument(String projectId, KnowledgeDocument document) {
        KnowledgeBaseManager projectKB = projectKnowledgeBases.get(projectId);
        if (projectKB == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }
        
        // 添加项目标签
        if (document.getTags() == null) {
            document.setTags(new String[]{"projectId:" + projectId});
        } else {
            String[] newTags = Arrays.copyOf(document.getTags(), document.getTags().length + 1);
            newTags[newTags.length - 1] = "projectId:" + projectId;
            document.setTags(newTags);
        }
        
        projectKB.addDocument(document);
        log.info("向项目 {} 添加文档: {}", projectId, document.getTitle());
    }
    
    /**
     * 向全局知识库添加文档（跨项目共享）
     */
    public void addGlobalDocument(KnowledgeDocument document) {
        knowledgeBaseManager.addDocument(document);
        log.info("向全局知识库添加文档: {}", document.getTitle());
    }
    
    /**
     * 智能检索（项目专属 + 全局共享）
     */
    public List<SearchResult> searchKnowledge(String projectId, String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        
        // 1. 检索项目专属知识库（优先级最高）
        KnowledgeBaseManager projectKB = projectKnowledgeBases.get(projectId);
        if (projectKB != null) {
            List<KnowledgeDocument> projectDocs = projectKB.searchRelevantDocuments(query, maxResults, 0.6);
            for (KnowledgeDocument doc : projectDocs) {
                results.add(new SearchResult(doc, doc.getRelevanceScore(), "project"));
            }
            log.debug("项目 {} 检索到 {} 条结果", projectId, projectDocs.size());
        }
        
        // 2. 检索全局知识库（补充）
        List<KnowledgeDocument> globalDocs = knowledgeBaseManager.searchRelevantDocuments(query, maxResults, 0.6);
        for (KnowledgeDocument doc : globalDocs) {
            results.add(new SearchResult(doc, doc.getRelevanceScore(), "global"));
        }
        log.debug("全局知识库检索到 {} 条结果", globalDocs.size());
        
        // 3. 按相关度排序
        results.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());
        
        // 4. 去重（基于文档ID）
        Set<String> seenIds = new HashSet<>();
        results = results.stream()
            .filter(r -> seenIds.add(r.getDocument().getId()))
            .limit(maxResults)
            .collect(Collectors.toList());
        
        log.info("项目 {} 查询 '{}' 最终返回 {} 条结果", projectId, query, results.size());
        
        return results;
    }
    
    /**
     * 跨项目检索（查找相似项目的解决方案）
     */
    public List<CrossProjectResult> searchAcrossProjects(String query, 
                                                         List<String> projectIds, 
                                                         int maxResults) {
        List<CrossProjectResult> allResults = new ArrayList<>();
        
        for (String projectId : projectIds) {
            KnowledgeBaseManager projectKB = projectKnowledgeBases.get(projectId);
            if (projectKB != null) {
                List<KnowledgeDocument> docs = projectKB.searchRelevantDocuments(query, maxResults, 0.6);
                
                for (KnowledgeDocument doc : docs) {
                    SearchResult searchResult = new SearchResult(doc, doc.getRelevanceScore(), "project");
                    CrossProjectResult crossResult = new CrossProjectResult(
                        projectId,
                        searchResult,
                        projectMetadataMap.get(projectId)
                    );
                    allResults.add(crossResult);
                }
            }
        }
        
        // 按相关度排序
        allResults.sort(Comparator.comparingDouble(r -> r.getSearchResult().getScore()).reversed());
        
        return allResults.stream()
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取项目列表
     */
    public List<ProjectMetadata> getAllProjects() {
        return new ArrayList<>(projectMetadataMap.values());
    }
    
    /**
     * 获取项目统计信息
     */
    public Map<String, Object> getProjectStats(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("projectId", projectId);
        stats.put("metadata", projectMetadataMap.get(projectId));
        
        KnowledgeBaseManager projectKB = projectKnowledgeBases.get(projectId);
        if (projectKB != null) {
            stats.put("knowledgeStats", projectKB.getKnowledgeBaseStats());
        }
        
        return stats;
    }
    
    /**
     * 项目配置
     */
    @Data
    public static class ProjectConfig {
        private String projectId;
        private String projectName;
        private String techStack;
        private String domain;
        private boolean enableGlobalSharing;
    }
    
    /**
     * 项目元数据
     */
    @Data
    public static class ProjectMetadata {
        private String projectId;
        private String projectName;
        private String techStack;
        private String domain;
        private long createdAt;
        private boolean enableGlobalSharing;
        
        public ProjectMetadata(ProjectConfig config) {
            this.projectId = config.getProjectId();
            this.projectName = config.getProjectName();
            this.techStack = config.getTechStack();
            this.domain = config.getDomain();
            this.enableGlobalSharing = config.isEnableGlobalSharing();
        }
    }
    
    /**
     * 搜索结果
     */
    @Data
    public static class SearchResult {
        private KnowledgeDocument document;
        private Double score;
        private String scope; // "project" or "global"
        
        public SearchResult(KnowledgeDocument document, Double score, String scope) {
            this.document = document;
            this.score = score;
            this.scope = scope;
        }
    }
    
    /**
     * 跨项目搜索结果
     */
    @Data
    public static class CrossProjectResult {
        private String projectId;
        private SearchResult searchResult;
        private ProjectMetadata projectMetadata;
        
        public CrossProjectResult(String projectId, SearchResult searchResult, ProjectMetadata projectMetadata) {
            this.projectId = projectId;
            this.searchResult = searchResult;
            this.projectMetadata = projectMetadata;
        }
    }
}
