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
        try {
            loadDefaultKnowledge();
            log.info("知识库系统初始化完成");
        } catch (Exception e) {
            log.warn("默认知识库加载失败（可能因 API Key 无效或网络问题），应用将继续启动: {}", e.getMessage());
        }
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
        
        // MCP 工具集成文档
        addDocument(new KnowledgeDocument(
            "mcp-tools-guide",
            "MCP 工具集成使用指南",
            "MCP（Model Context Protocol）工具集成为 Agent 提供了强大的执行能力。\n\n" +
            "**CodeFileTool - 文件操作工具**\n" +
            "功能：\n" +
            "1. readFile(filePath) - 读取文件内容\n" +
            "2. writeFile(filePath, content) - 写入文件\n" +
            "3. listFiles(directoryPath) - 列出目录文件\n" +
            "4. listFilesRecursively(directoryPath, extension) - 递归列出文件\n" +
            "5. fileExists(path) - 检查文件是否存在\n\n" +
            "**CodeAnalysisTool - 代码分析工具**\n" +
            "功能：\n" +
            "1. analyzeCodeStats(code) - 代码统计（行数、注释率等）\n" +
            "2. findTodoAndFixme(code) - 检测 TODO/FIXME 标记\n" +
            "3. estimateMethodComplexity(code) - 方法复杂度评估\n\n" +
            "**KnowledgeBaseTool - 知识库检索工具**\n" +
            "功能：\n" +
            "1. searchKnowledge(query) - 语义检索相关知识\n" +
            "2. getDocumentsByCategory(category) - 按分类获取文档\n" +
            "3. searchDocumentsByTags(tags) - 按标签搜索\n" +
            "4. getKnowledgeBaseStats() - 获取统计信息\n\n" +
            "使用场景：Agent 在执行任务时自动调用这些工具来读写文件、分析代码、检索知识",
            "技术文档",
            new String[]{"mcp", "tools", "code-analysis", "file-operations"},
            "system",
            null,
            LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // Skill 技能管理系统文档
        addDocument(new KnowledgeDocument(
            "skill-management-system",
            "Skill 技能管理系统详解",
            "Skill 系统让 Agent 具备结构化的专业能力管理。\n\n" +
            "**Skill 实体结构**\n" +
            "- id: 技能唯一标识\n" +
            "- name: 技能名称\n" +
            "- description: 技能描述\n" +
            "- category: 技能分类（框架、设计模式、数据库等）\n" +
            "- content: 技能详细内容（Prompt 模板或使用指南）\n" +
            "- keywords: 关联关键词（用于检索）\n" +
            "- proficiencyLevel: 熟练程度（1-5）\n" +
            "- usageCount: 使用次数统计\n\n" +
            "**SkillManager 核心功能**\n" +
            "1. registerSkill(skill) - 注册新技能\n" +
            "2. searchSkills(query) - 根据关键词搜索技能\n" +
            "3. getSkillsByCategory(category) - 按分类获取\n" +
            "4. recordSkillUsage(skillId) - 记录使用\n" +
            "5. updateSkill(skillId, updatedSkill) - 更新技能\n" +
            "6. deleteSkill(skillId) - 删除技能\n" +
            "7. getSkillStatistics() - 获取统计信息\n\n" +
            "**预置技能**\n" +
            "1. java-spring-boot: Spring Boot 开发（熟练度 5）\n" +
            "2. python-fastapi: Python FastAPI 开发（熟练度 5）\n" +
            "3. react-typescript: React + TypeScript 前端开发（熟练度 5）\n" +
            "4. design-patterns: 设计模式应用（熟练度 5）\n" +
            "5. sql-optimization: SQL 优化（熟练度 4）\n\n" +
            "API 接口：\n" +
            "- GET /api/ai-programming-agent/skills/search?query=xxx\n" +
            "- GET /api/ai-programming-agent/skills/all\n" +
            "- POST /api/ai-programming-agent/skills/add",
            "技术文档",
            new String[]{"skill", "capability", "management", "proficiency"},
            "system",
            null,
            LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 知识库 RAG 系统文档
        addDocument(new KnowledgeDocument(
            "rag-knowledge-base-system",
            "知识库 RAG 系统完整架构",
            "RAG（Retrieval-Augmented Generation）是本系统的核心功能。\n\n" +
            "**KnowledgeDocument 实体**\n" +
            "- id: 文档唯一标识\n" +
            "- title: 文档标题\n" +
            "- content: 文档内容\n" +
            "- category: 文档分类（技术文档、最佳实践、项目规范等）\n" +
            "- tags: 标签数组（用于检索）\n" +
            "- source: 来源（文件路径、URL、手动输入等）\n" +
            "- embeddingId: 向量存储中的 ID\n" +
            "- accessCount: 访问次数\n" +
            "- relevanceScore: 相关度评分（检索时动态计算）\n\n" +
            "**KnowledgeBaseManager 核心流程**\n" +
            "1. 文档添加流程：\n" +
            "   a. 使用 DocumentSplitter 将文档分块（500 tokens/块，50 tokens 重叠）\n" +
            "   b. 为每个分块生成 Embedding 向量\n" +
            "   c. 存储到 EmbeddingStore（向量数据库）\n" +
            "   d. 记录元数据到 documentRegistry\n\n" +
            "2. 语义检索流程：\n" +
            "   a. 将查询文本生成 Embedding 向量\n" +
            "   b. 在向量数据库中执行相似度搜索\n" +
            "   c. 返回相关度 >= minScore（默认 0.6）的文档\n" +
            "   d. 按相关度排序，返回最多 maxResults（默认 5）个结果\n\n" +
            "**Embedding 配置**\n" +
            "- 模型：text-embedding-v3（通义千问）\n" +
            "- 存储：InMemoryEmbeddingStore（开发环境）\n" +
            "- 生产建议：Milvus、Pinecone、Qdrant\n\n" +
            "**预置知识文档**\n" +
            "1. LangChain4j 开发指南\n" +
            "2. Spring Boot 最佳实践\n" +
            "3. MCP 工具集成使用指南\n" +
            "4. Skill 技能管理系统详解\n" +
            "5. RAG 知识库系统架构（本文档）\n" +
            "6. 自主学习能力系统\n" +
            "7. AI 监管系统\n\n" +
            "API 接口：\n" +
            "- POST /api/ai-programming-agent/knowledge/add\n" +
            "- GET /api/ai-programming-agent/knowledge/search?query=xxx\n" +
            "- GET /api/ai-programming-agent/knowledge/stats",
            "技术文档",
            new String[]{"rag", "knowledge-base", "embedding", "vector-search", "semantic-search"},
            "system",
            null,
            LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 自主学习能力文档
        addDocument(new KnowledgeDocument(
            "self-learning-system",
            "自主学习能力系统实现",
            "自主学习能力让 Agent 从交互中持续改进。\n\n" +
            "**LearningExperience 实体**\n" +
            "- query: 用户查询/问题\n" +
            "- response: Agent 的响应\n" +
            "- userFeedback: 用户反馈文本\n" +
            "- feedbackScore: 反馈分数（1-5）\n" +
            "- usedSkills: 使用的技能 ID 列表\n" +
            "- retrievedKnowledge: 检索到的知识文档 ID 列表\n" +
            "- usedTools: 使用的工具列表\n" +
            "- tokensUsed: Token 使用统计\n" +
            "- learnedImprovement: 学习到的改进点\n" +
            "- category: 经验分类\n" +
            "- isReviewed: 是否已审核\n\n" +
            "**SelfLearningManager 核心功能**\n" +
            "1. recordExperience(experience) - 记录交互经验\n" +
            "2. learnFromPositiveExperience(experience) - 从正面反馈学习\n" +
            "   - 提取成功的技能组合模式\n" +
            "   - 将优秀响应转化为知识文档\n" +
            "3. analyzeNegativeExperience(experience) - 分析负面反馈\n" +
            "   - 记录避免模式\n" +
            "   - 标记工具使用可能不当\n" +
            "4. getImprovementPatterns(category) - 获取改进模式\n" +
            "5. learnNewKnowledge(knowledge, category) - 手动学习新知识\n" +
            "6. reviewExperience(experienceId, approved) - 审核经验\n" +
            "7. generateLearningReport() - 生成学习报告\n\n" +
            "**学习策略**\n" +
            "- 反馈分数 >= 4：自动转化为知识库文档\n" +
            "- 反馈分数 <= 2：分析改进模式并记录警告\n" +
            "- 所有经验：记录到经验库供后续分析\n\n" +
            "**统计指标**\n" +
            "- 总经验数\n" +
            "- 平均反馈分数\n" +
            "- 正面经验数量（>=4 分）\n" +
            "- 负面经验数量（<=2 分）\n" +
            "- 改进模式数量\n\n" +
            "API 接口：\n" +
            "- POST /api/ai-programming-agent/learning/feedback\n" +
            "- POST /api/ai-programming-agent/learning/learn\n" +
            "- GET /api/ai-programming-agent/learning/report",
            "技术文档",
            new String[]{"self-learning", "feedback", "improvement", "experience"},
            "system",
            null,
            LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // AI 监管系统文档
        addDocument(new KnowledgeDocument(
            "ai-supervision-system",
            "AI 监管系统完整实现",
            "AI 监管系统防止 Token 异常使用和成本控制。\n\n" +
            "**TokenUsageRecord 实体**\n" +
            "- agentName: Agent 名称\n" +
            "- userId: 用户 ID\n" +
            "- requestType: 请求类型（chat、code-review、question 等）\n" +
            "- inputTokens: 输入 token 数\n" +
            "- outputTokens: 输出 token 数\n" +
            "- totalTokens: 总 token 数\n" +
            "- durationMs: 请求耗时（毫秒）\n" +
            "- alertTriggered: 是否触发警报\n" +
            "- alertReason: 警报原因\n\n" +
            "**AiSupervisor 核心功能**\n" +
            "1. recordTokenUsage(record) - 记录 Token 使用\n" +
            "2. checkLimits(userId, requestedTokens) - 检查是否超过限制\n" +
            "   - 单次请求限制：maxTokensPerRequest（默认 10000）\n" +
            "   - 每小时限制：maxTokensPerHour（默认 100000）\n" +
            "   - 每日限制：maxTokensPerDay（默认 1000000）\n" +
            "   - 并发请求限制：maxConcurrentRequests（默认 10）\n" +
            "3. incrementConcurrentRequests() - 增加并发计数\n" +
            "4. decrementConcurrentRequests() - 减少并发计数\n" +
            "5. checkAlerts(record) - 检查并触发警报\n" +
            "6. getTokenUsageStats() - 获取使用统计\n" +
            "7. generateSupervisionReport() - 生成监管报告\n\n" +
            "**警报机制**\n" +
            "- 触发条件：使用率达到 alertThreshold（默认 80%）\n" +
            "- 警报类型：\n" +
            "  a. 每小时使用率过高\n" +
            "  b. 每日使用率过高\n" +
            "  c. 单次请求 token 数异常高\n" +
            "- 拒绝策略：超过限制直接拒绝请求并记录警报\n\n" +
            "**配置参数（application.yml）**\n" +
            "```yaml\n" +
            "ai.supervisor.max-tokens-per-request=10000\n" +
            "ai.supervisor.max-tokens-per-hour=100000\n" +
            "ai.supervisor.max-tokens-per-day=1000000\n" +
            "ai.supervisor.max-concurrent-requests=10\n" +
            "ai.supervisor.alert-threshold=0.8\n" +
            "```\n\n" +
            "**统计指标**\n" +
            "- 总请求数\n" +
            "- 总 Token 使用\n" +
            "- 当前小时使用 / 限制\n" +
            "- 当前日使用 / 限制\n" +
            "- 平均每次请求 Token 数\n" +
            "- 当前并发请求数\n" +
            "- 警报数量\n\n" +
            "API 接口：\n" +
            "- GET /api/ai-programming-agent/supervisor/stats\n" +
            "- GET /api/ai-programming-agent/supervisor/report\n" +
            "- GET /api/ai-programming-agent/supervisor/alerts",
            "技术文档",
            new String[]{"supervision", "token-monitoring", "cost-control", "alert", "rate-limiting"},
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
        List<String> embeddingIds = new ArrayList<>();
        for (TextSegment segment : segments) {
            // 为每个 TextSegment 生成 embedding 并存储
            Embedding embedding = embeddingModel.embed(segment.text()).content();
            String id = embeddingStore.add(embedding, segment);
            embeddingIds.add(id);
        }
        
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
            .map(segment -> {
                dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
                metadata.put("docId", document.getId());
                metadata.put("title", document.getTitle());
                metadata.put("category", document.getCategory());
                metadata.put("source", document.getSource());
                return TextSegment.from(segment.text(), metadata);
            })
            .collect(Collectors.toList());
    }
}

