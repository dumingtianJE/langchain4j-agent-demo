package com.yourcompany.langchain4j.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
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
        // 初始化全局共享知识库
        globalKnowledgeBase = new KnowledgeBaseManager(embeddingModel);
        globalKnowledgeBase.initializeKnowledge();
        log.info("全局共享知识库初始化完成");
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
        KnowledgeBaseManager projectKB = new KnowledgeBaseManager(embeddingModel);
        projectKB.initializeKnowledge();
        projectKnowledgeBases.put(projectId, projectKB);
        
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
        document.getMetadata().put("projectId", projectId);
        document.getMetadata().put("scope", "project");
        
        projectKB.addDocument(document);
        log.info("向项目 {} 添加文档: {}", projectId, document.getTitle());
    }
    
    /**
     * 向全局知识库添加文档（跨项目共享）
     */
    public void addGlobalDocument(KnowledgeDocument document) {
        document.getMetadata().put("scope", "global");
        globalKnowledgeBase.addDocument(document);
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
            List<SearchResult> projectResults = searchInKB(projectKB, query, maxResults, "project");
            results.addAll(projectResults);
            log.debug("项目 {} 检索到 {} 条结果", projectId, projectResults.size());
        }
        
        // 2. 检索全局知识库（补充）
        List<SearchResult> globalResults = searchInKB(globalKnowledgeBase, query, maxResults, "global");
        results.addAll(globalResults);
        log.debug("全局知识库检索到 {} 条结果", globalResults.size());
        
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
                List<SearchResult> results = searchInKB(projectKB, query, maxResults, "project");
                
                for (SearchResult result : results) {
                    CrossProjectResult crossResult = new CrossProjectResult(
                        projectId,
                        result,
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
     * 在指定知识库中检索
     */
    private List<SearchResult> searchInKB(KnowledgeBaseManager kb, 
                                          String query, 
                                          int maxResults, 
                                          String scope) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(0.7)
                .build();
            
            EmbeddingSearchResult<TextSegment> searchResult = kb.search(request);
            
            return searchResult.matches().stream()
                .map(match -> new SearchResult(
                    match.embedded(),
                    match.score(),
                    scope
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取项目统计信息
     */
    public ProjectStats getProjectStats(String projectId) {
        KnowledgeBaseManager projectKB = projectKnowledgeBases.get(projectId);
        if (projectKB == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }
        
        ProjectMetadata metadata = projectMetadataMap.get(projectId);
        
        return new ProjectStats(
            projectId,
            metadata,
            projectKB.getDocumentCount(),
            projectKB.getLastUpdateTime()
        );
    }
    
    /**
     * 项目配置
     */
    public static class ProjectConfig {
        private String projectId;
        private String projectName;
        private String techStack;  // Spring Boot, React, Python 等
        private String domain;     // 电商、金融、教育 等
        private List<String> tags;
        private boolean enableGlobalKB = true;  // 是否启用全局知识库
        
        // getters and setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getTechStack() { return techStack; }
        public void setTechStack(String techStack) { this.techStack = techStack; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public boolean isEnableGlobalKB() { return enableGlobalKB; }
        public void setEnableGlobalKB(boolean enableGlobalKB) { this.enableGlobalKB = enableGlobalKB; }
    }
    
    /**
     * 项目元数据
     */
    public static class ProjectMetadata {
        private String projectId;
        private String projectName;
        private String techStack;
        private String domain;
        private List<String> tags;
        private long createdAt;
        private int documentCount;
        private long lastUpdateTime;
        
        public ProjectMetadata(ProjectConfig config) {
            this.projectId = config.getProjectId();
            this.projectName = config.getProjectName();
            this.techStack = config.getTechStack();
            this.domain = config.getDomain();
            this.tags = config.getTags();
        }
        
        // getters and setters
        public String getProjectId() { return projectId; }
        public String getProjectName() { return projectName; }
        public String getTechStack() { return techStack; }
        public String getDomain() { return domain; }
        public List<String> getTags() { return tags; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public int getDocumentCount() { return documentCount; }
        public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }
        public long getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    }
    
    /**
     * 检索结果
     */
    public static class SearchResult {
        private TextSegment document;
        private double score;
        private String scope;  // project or global
        
        public SearchResult(TextSegment document, double score, String scope) {
            this.document = document;
            this.score = score;
            this.scope = scope;
        }
        
        public TextSegment getDocument() { return document; }
        public double getScore() { return score; }
        public String getScope() { return scope; }
    }
    
    /**
     * 跨项目检索结果
     */
    public static class CrossProjectResult {
        private String projectId;
        private SearchResult searchResult;
        private ProjectMetadata projectMetadata;
        
        public CrossProjectResult(String projectId, SearchResult searchResult, ProjectMetadata projectMetadata) {
            this.projectId = projectId;
            this.searchResult = searchResult;
            this.projectMetadata = projectMetadata;
        }
        
        public String getProjectId() { return projectId; }
        public SearchResult getSearchResult() { return searchResult; }
        public ProjectMetadata getProjectMetadata() { return projectMetadata; }
    }
    
    /**
     * 项目统计
     */
    public static class ProjectStats {
        private String projectId;
        private ProjectMetadata metadata;
        private int documentCount;
        private long lastUpdateTime;
        
        public ProjectStats(String projectId, ProjectMetadata metadata, 
                           int documentCount, long lastUpdateTime) {
            this.projectId = projectId;
            this.metadata = metadata;
            this.documentCount = documentCount;
            this.lastUpdateTime = lastUpdateTime;
        }
        
        // getters
        public String getProjectId() { return projectId; }
        public ProjectMetadata getMetadata() { return metadata; }
        public int getDocumentCount() { return documentCount; }
        public long getLastUpdateTime() { return lastUpdateTime; }
    }
}
