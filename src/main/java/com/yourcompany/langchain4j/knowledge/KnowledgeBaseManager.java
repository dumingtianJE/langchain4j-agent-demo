package com.yourcompany.langchain4j.knowledge;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识库管理器（RAG 核心实现）
 * 负责文档的存储、检索和向量相似度搜索
 */
@Slf4j
@Service
public class KnowledgeBaseManager {
    
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    
    // 文档元数据存储
    private final Map<String, KnowledgeDocument> documentRegistry = new ConcurrentHashMap<>();
    
    // 文档分块器
    private final DocumentSplitter documentSplitter;
    
    public KnowledgeBaseManager(EmbeddingStore<TextSegment> embeddingStore, 
                                EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.documentSplitter = DocumentSplitters.recursive(500, 50);
    }
    
    @PostConstruct
    public void initializeKnowledgeBase() {
        log.info("知识库系统初始化完成");
        loadDefaultKnowledge();
    }
    
    /**
     * 加载默认知识库内容
     */
    private void loadDefaultKnowledge() {
        // 示例：LangChain4j 使用文档
        addDocument(new KnowledgeDocument(
            "langchain4j-guide",
            "LangChain4j 开发指南",
            "LangChain4j 是一个用于 Java 的 LLM 集成框架。\n\n" +
            "核心概念：\n" +
            "1. AiService - 声明式 AI 服务接口\n" +
            "2. Tool - 工具注解，让 LLM 可以调用方法\n" +
            "3. ChatMemory - 对话记忆管理\n" +
            "4. EmbeddingModel - 文本向量化\n" +
            "5. EmbeddingStore - 向量存储和检索\n\n" +
            "使用步骤：\n" +
            "1. 定义 @AiService 接口\n" +
            "2. 使用 @SystemMessage 定义系统提示\n" +
            "3. 使用 @Tool 注解工具方法\n" +
            "4. 配置 ChatModel 和 EmbeddingModel",
            "技术文档",
            new String[]{"langchain4j", "ai", "llm", "java"},
            "system",
            null,
            LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // Spring Boot 最佳实践
        addDocument(new KnowledgeDocument(
            "springboot-best-practices",
            "Spring Boot 最佳实践",
            "Spring Boot 开发最佳实践：\n\n" +
            "1. 项目结构\n" +
            "   - 按功能模块分包\n" +
            "   - Controller -> Service -> Repository\n\n" +
            "2. 配置管理\n" +
            "   - 使用 application.yml\n" +
            "   - 多环境配置（dev/test/prod）\n" +
            "   - 使用 @ConfigurationProperties\n\n" +
            "3. 异常处理\n" +
            "   - 统一异常处理器 @ControllerAdvice\n" +
            "   - 自定义异常类\n" +
            "   - 标准化错误响应\n\n" +
            "4. 性能优化\n" +
            "   - 启用压缩\n" +
            "   - 连接池配置\n" +
            "   - 缓存策略",
            "最佳实践",
            new String[]{"spring-boot", "best-practices", "java", "backend"},
            "system",
            null,
            LocalDateTime.now(),
            null,
            0,
            null
        ));
    }
    
    /**
     * 添加文档到知识库
     */
    public String addDocument(KnowledgeDocument document) {
        if (document.getId() == null) {
            document.setId(UUID.randomUUID().toString());
        }
        document.setCreatedAt(LocalDateTime.now());
        document.setAccessCount(0);
        
        // 将文档分块并生成 embedding
        List<TextSegment> segments = splitAndEmbed(document);
        
        // 存储到向量数据库
        List<String> embeddingIds = embeddingStore.addAll(segments);
        
        // 记录第一个 embedding ID
        if (!embeddingIds.isEmpty()) {
            document.setEmbeddingId(embeddingIds.get(0));
        }
        
        documentRegistry.put(document.getId(), document);
        log.info("文档已添加到知识库: {} - {}", document.getId(), document.getTitle());
        
        return document.getId();
    }
    
    /**
     * 批量添加文档
     */
    public List<String> addDocuments(List<KnowledgeDocument> documents) {
        return documents.stream()
            .map(this::addDocument)
            .collect(Collectors.toList());
    }
    
    /**
     * 语义检索 - 基于向量相似度搜索相关文档
     * 
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param minScore 最小相关度阈值
     * @return 相关文档列表
     */
    public List<KnowledgeDocument> searchRelevantDocuments(String query, 
                                                           int maxResults, 
                                                           double minScore) {
        log.debug("检索知识库: query={}, maxResults={}", query, maxResults);
        
        // 生成查询的 embedding
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // 向量相似度搜索
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .minScore(minScore)
            .build();
        
        EmbeddingSearchResult<TextSegment> searchResult = 
            embeddingStore.search(searchRequest);
        
        // 提取相关文档
        Map<String, KnowledgeDocument> relevantDocs = new LinkedHashMap<>();
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            TextSegment segment = match.embedded();
            String docId = segment.metadata().getString("docId");
            
            KnowledgeDocument doc = documentRegistry.get(docId);
            if (doc != null) {
                doc.setRelevanceScore(match.score());
                doc.setLastAccessedAt(LocalDateTime.now());
                doc.setAccessCount(doc.getAccessCount() + 1);
                relevantDocs.put(docId, doc);
            }
        }
        
        log.info("检索到 {} 个相关文档", relevantDocs.size());
        return new ArrayList<>(relevantDocs.values());
    }
    
    /**
     * 简化检索（使用默认参数）
     */
    public List<KnowledgeDocument> searchRelevantDocuments(String query) {
        return searchRelevantDocuments(query, 5, 0.6);
    }
    
    /**
     * 根据 ID 获取文档
     */
    public Optional<KnowledgeDocument> getDocumentById(String id) {
        return Optional.ofNullable(documentRegistry.get(id));
    }
    
    /**
     * 根据分类获取文档
     */
    public List<KnowledgeDocument> getDocumentsByCategory(String category) {
        return documentRegistry.values().stream()
            .filter(doc -> doc.getCategory().equals(category))
            .collect(Collectors.toList());
    }
    
    /**
     * 根据标签检索文档
     */
    public List<KnowledgeDocument> searchByTags(String[] tags) {
        return documentRegistry.values().stream()
            .filter(doc -> {
                if (doc.getTags() == null) return false;
                return Arrays.stream(doc.getTags())
                    .anyMatch(tag -> Arrays.asList(tags).contains(tag));
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 更新文档
     */
    public boolean updateDocument(String docId, KnowledgeDocument updatedDoc) {
        KnowledgeDocument existing = documentRegistry.get(docId);
        if (existing != null) {
            existing.setTitle(updatedDoc.getTitle());
            existing.setContent(updatedDoc.getContent());
            existing.setCategory(updatedDoc.getCategory());
            existing.setTags(updatedDoc.getTags());
            existing.setUpdatedAt(LocalDateTime.now());
            return true;
        }
        return false;
    }
    
    /**
     * 删除文档
     */
    public boolean deleteDocument(String docId) {
        KnowledgeDocument doc = documentRegistry.remove(docId);
        if (doc != null && doc.getEmbeddingId() != null) {
            embeddingStore.remove(doc.getEmbeddingId());
            log.info("文档已从知识库删除: {}", docId);
            return true;
        }
        return false;
    }
    
    /**
     * 获取知识库统计信息
     */
    public Map<String, Object> getKnowledgeBaseStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documentRegistry.size());
        stats.put("categories", documentRegistry.values().stream()
            .map(KnowledgeDocument::getCategory)
            .distinct()
            .collect(Collectors.toList()));
        stats.put("totalAccessCount", documentRegistry.values().stream()
            .mapToInt(KnowledgeDocument::getAccessCount)
            .sum());
        
        return stats;
    }
    
    /**
     * 将文档分块并生成 embedding
     */
    private List<TextSegment> splitAndEmbed(KnowledgeDocument document) {
        Document doc = Document.from(document.getContent());
        List<TextSegment> segments = documentSplitter.split(doc);
        
        // 为每个分块添加元数据
        return segments.stream()
            .map(segment -> TextSegment.from(
                segment.text(),
                dev.langchain4j.data.document.Metadata.metadata()
                    .put("docId", document.getId())
                    .put("title", document.getTitle())
                    .put("category", document.getCategory())
                    .put("source", document.getSource())
            ))
            .collect(Collectors.toList());
    }
}

