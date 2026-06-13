package com.yourcompany.langchain4j.learning;

import com.yourcompany.langchain4j.entity.LearningExperienceEntity;
import com.yourcompany.langchain4j.knowledge.KnowledgeBaseManager;
import com.yourcompany.langchain4j.knowledge.KnowledgeDocument;
import com.yourcompany.langchain4j.repository.LearningExperienceRepository;
import com.yourcompany.langchain4j.skill.SkillManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 自主学习管理器
 * Agent 通过用户反馈持续改进和优化
 * 支持 JPA 持久化，重启后不会丢失学习成果
 */
@Slf4j
@Service
public class SelfLearningManager {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    private final SkillManager skillManager;
    private final LearningExperienceRepository learningExperienceRepository;
    
    // 内存缓存，加速查询
    private final Map<String, LearningExperience> experienceStore = new ConcurrentHashMap<>();
    
    // 改进模式库（从经验中提取的改进模式）
    private final List<String> improvementPatterns = Collections.synchronizedList(new ArrayList<>());
    
    public SelfLearningManager(KnowledgeBaseManager knowledgeBaseManager, 
                               SkillManager skillManager,
                               LearningExperienceRepository learningExperienceRepository) {
        this.knowledgeBaseManager = knowledgeBaseManager;
        this.skillManager = skillManager;
        this.learningExperienceRepository = learningExperienceRepository;
    }
    
    @PostConstruct
    public void initializeLearningSystem() {
        // 从数据库加载历史学习经验到内存缓存
        try {
            List<LearningExperienceEntity> entities = learningExperienceRepository.findAll();
            for (LearningExperienceEntity entity : entities) {
                experienceStore.put(entity.getId(), toDomain(entity));
            }
            log.info("自主学习系统初始化完成，已从数据库加载 {} 条历史经验", entities.size());
        } catch (Exception e) {
            log.warn("从数据库加载学习经验失败，将以空缓存启动: {}", e.getMessage());
            log.info("自主学习系统初始化完成（内存模式）");
        }
    }
    
    /**
     * 记录交互经验并持久化到数据库
     */
    public String recordExperience(LearningExperience experience) {
        if (experience.getId() == null) {
            experience.setId(UUID.randomUUID().toString());
        }
        experience.setCreatedAt(LocalDateTime.now());
        experience.setIsReviewed(false);
        
        experienceStore.put(experience.getId(), experience);
        
        // 持久化到数据库
        persistToDatabase(experience);
        
        log.info("已记录学习经验: {} - 反馈分数: {}", 
            experience.getId(), experience.getFeedbackScore());
        
        // 如果反馈分数较高，自动学习
        if (experience.getFeedbackScore() != null && experience.getFeedbackScore() >= 4) {
            learnFromPositiveExperience(experience);
        }
        
        // 如果反馈分数较低，分析改进
        if (experience.getFeedbackScore() != null && experience.getFeedbackScore() <= 2) {
            analyzeNegativeExperience(experience);
        }
        
        return experience.getId();
    }
    
    /**
     * 将经验持久化到数据库
     */
    private void persistToDatabase(LearningExperience experience) {
        try {
            LearningExperienceEntity entity = toEntity(experience);
            learningExperienceRepository.save(entity);
            log.debug("学习经验已持久化到数据库: {}", experience.getId());
        } catch (Exception e) {
            log.warn("学习经验持久化失败（内存缓存已保存）: {}", e.getMessage());
        }
    }
    
    /**
     * 从正面反馈中学习
     */
    private void learnFromPositiveExperience(LearningExperience experience) {
        log.info("从正面经验中学习: {}", experience.getId());
        
        // 提取成功的模式
        if (experience.getUsedSkills() != null && experience.getUsedSkills().length > 0) {
            // 记录成功的技能组合
            String pattern = String.format("成功模式: 使用技能 [%s] 处理 %s 类问题",
                String.join(", ", experience.getUsedSkills()),
                experience.getCategory());
            improvementPatterns.add(pattern);
        }
        
        // 将成功的响应转化为知识
        if (experience.getLearnedImprovement() != null) {
            KnowledgeDocument knowledge = new KnowledgeDocument();
            knowledge.setId(UUID.randomUUID().toString());
            knowledge.setTitle(String.format("最佳实践: %s", experience.getQuery()));
            knowledge.setContent(experience.getLearnedImprovement());
            knowledge.setCategory(experience.getCategory() != null ? experience.getCategory() : "最佳实践");
            knowledge.setTags(experience.getUsedSkills());
            knowledge.setSource("自主学习");
            knowledge.setCreatedAt(LocalDateTime.now());
            knowledge.setAccessCount(0);
            
            knowledgeBaseManager.addDocument(knowledge);
            log.info("已将成功经验转化为知识库文档");
        }
    }
    
    /**
     * 分析负面反馈并改进
     */
    private void analyzeNegativeExperience(LearningExperience experience) {
        log.warn("分析负面经验: {} - 反馈: {}", 
            experience.getId(), experience.getUserFeedback());
        
        // 提取改进点
        String improvement = String.format("避免: %s - 用户反馈: %s",
            experience.getQuery(),
            experience.getUserFeedback());
        improvementPatterns.add(improvement);
        
        // 如果响应使用了某些工具但用户不满意，记录警告
        if (experience.getUsedTools() != null) {
            log.warn("工具使用可能不当: {}", String.join(", ", experience.getUsedTools()));
        }
    }
    
    /**
     * 获取学习到的改进模式
     */
    public List<String> getImprovementPatterns(String category) {
        if (category == null) {
            return new ArrayList<>(improvementPatterns);
        }
        
        return improvementPatterns.stream()
            .filter(pattern -> pattern.contains(category))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取经验统计信息
     */
    public Map<String, Object> getLearningStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExperiences", experienceStore.size());
        
        // 平均反馈分数
        double avgScore = experienceStore.values().stream()
            .filter(e -> e.getFeedbackScore() != null)
            .mapToInt(LearningExperience::getFeedbackScore)
            .average()
            .orElse(0.0);
        stats.put("averageFeedbackScore", String.format("%.2f", avgScore));
        
        // 高反馈经验数量
        long positiveCount = experienceStore.values().stream()
            .filter(e -> e.getFeedbackScore() != null && e.getFeedbackScore() >= 4)
            .count();
        stats.put("positiveExperiences", positiveCount);
        
        // 低反馈经验数量
        long negativeCount = experienceStore.values().stream()
            .filter(e -> e.getFeedbackScore() != null && e.getFeedbackScore() <= 2)
            .count();
        stats.put("negativeExperiences", negativeCount);
        
        // 改进模式数量
        stats.put("improvementPatterns", improvementPatterns.size());
        
        return stats;
    }
    
    /**
     * 获取最近的学习经验
     */
    public List<LearningExperience> getRecentExperiences(int limit) {
        return experienceStore.values().stream()
            .sorted(Comparator.comparing(LearningExperience::getCreatedAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 根据反馈分数筛选经验
     */
    public List<LearningExperience> getExperiencesByFeedbackScore(int minScore, int maxScore) {
        return experienceStore.values().stream()
            .filter(e -> e.getFeedbackScore() != null && 
                        e.getFeedbackScore() >= minScore && 
                        e.getFeedbackScore() <= maxScore)
            .sorted(Comparator.comparing(LearningExperience::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * 手动学习新知识
     */
    public String learnNewKnowledge(String knowledge, String category) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setTitle(String.format("手动学习: %s", 
            knowledge.length() > 50 ? knowledge.substring(0, 50) + "..." : knowledge));
        doc.setContent(knowledge);
        doc.setCategory(category);
        doc.setSource("手动输入");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setAccessCount(0);
        
        String docId = knowledgeBaseManager.addDocument(doc);
        log.info("手动学习新知识: {} - 分类: {}", docId, category);
        
        return docId;
    }
    
    /**
     * 审核经验
     */
    public boolean reviewExperience(String experienceId, boolean approved) {
        LearningExperience experience = experienceStore.get(experienceId);
        if (experience != null) {
            experience.setIsReviewed(true);
            log.info("经验已审核: {} - 批准: {}", experienceId, approved);
            return true;
        }
        return false;
    }
    
    /**
     * 构建学习上下文（注入 Agent Prompt 的闭环反馈）
     * 根据用户查询关键词，提取相关的改进模式和历史经验教训，
     * 以精简的格式供 Agent 参考，避免重复犯同样的错误。
     *
     * @param query 用户当前查询
     * @param maxPatterns 最多返回的改进模式数
     * @return 学习上下文摘要，如果没有相关经验则返回 null
     */
    public String buildLearningContext(String query, int maxPatterns) {
        if (query == null || query.isBlank()) return null;
        
        // 从查询中提取关键词（简单分词：按空格和标点分割，过滤短词）
        String[] words = query.toLowerCase().split("[\\s,;.!?，；。！？]+");
        Set<String> keywords = new HashSet<>();
        for (String word : words) {
            if (word.length() >= 2) keywords.add(word);
        }
        if (keywords.isEmpty()) return null;
        
        // 匹配相关改进模式
        List<String> relevantPatterns = new ArrayList<>();
        for (String pattern : improvementPatterns) {
            String lower = pattern.toLowerCase();
            for (String kw : keywords) {
                if (lower.contains(kw)) {
                    relevantPatterns.add(pattern);
                    break;
                }
            }
            if (relevantPatterns.size() >= maxPatterns) break;
        }
        
        // 匹配相关负面经验（避免重蹈覆辙）
        List<String> warnings = new ArrayList<>();
        for (LearningExperience exp : experienceStore.values()) {
            if (exp.getFeedbackScore() != null && exp.getFeedbackScore() <= 2) {
                String q = exp.getQuery() != null ? exp.getQuery().toLowerCase() : "";
                for (String kw : keywords) {
                    if (q.contains(kw)) {
                        String warning = String.format("避免: %s (用户反馈: %s)",
                                exp.getQuery() != null ? truncate(exp.getQuery(), 80) : "",
                                exp.getUserFeedback() != null ? truncate(exp.getUserFeedback(), 80) : "");
                        warnings.add(warning);
                        break;
                    }
                }
                if (warnings.size() >= 3) break;
            }
        }
        
        if (relevantPatterns.isEmpty() && warnings.isEmpty()) return null;
        
        StringBuilder sb = new StringBuilder("【历史学习经验（供参考）】\n");
        if (!relevantPatterns.isEmpty()) {
            for (String p : relevantPatterns) {
                sb.append("  • ").append(p).append("\n");
            }
        }
        if (!warnings.isEmpty()) {
            sb.append("⚠️ 历史教训:\n");
            for (String w : warnings) {
                sb.append("  • ").append(w).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
    
    /**
     * 生成学习报告
     */
    public Map<String, Object> generateLearningReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("statistics", getLearningStatistics());
        report.put("recentExperiences", getRecentExperiences(10));
        report.put("improvementPatterns", getImprovementPatterns(null));
        report.put("generatedAt", LocalDateTime.now());
        
        return report;
    }
    
    // ==================== 对象转换工具 ====================
    
    /**
     * LearningExperienceEntity -> LearningExperience
     */
    private LearningExperience toDomain(LearningExperienceEntity entity) {
        LearningExperience domain = new LearningExperience();
        domain.setId(entity.getId());
        domain.setQuery(entity.getQuery());
        domain.setResponse(entity.getResponse());
        domain.setUserFeedback(entity.getUserFeedback());
        domain.setFeedbackScore(entity.getFeedbackScore());
        domain.setUsedSkills(entity.getUsedSkills());
        domain.setRetrievedKnowledge(entity.getRetrievedKnowledge());
        domain.setUsedTools(entity.getUsedTools());
        domain.setTokensUsed(entity.getTokensUsed());
        domain.setLearnedImprovement(entity.getLearnedImprovement());
        domain.setCategory(entity.getCategory());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setIsReviewed(entity.getIsReviewed());
        return domain;
    }
    
    /**
     * LearningExperience -> LearningExperienceEntity
     */
    private LearningExperienceEntity toEntity(LearningExperience domain) {
        LearningExperienceEntity entity = new LearningExperienceEntity();
        entity.setId(domain.getId());
        entity.setQuery(domain.getQuery());
        entity.setResponse(domain.getResponse());
        entity.setUserFeedback(domain.getUserFeedback());
        entity.setFeedbackScore(domain.getFeedbackScore());
        entity.setUsedSkills(domain.getUsedSkills());
        entity.setRetrievedKnowledge(domain.getRetrievedKnowledge());
        entity.setUsedTools(domain.getUsedTools());
        entity.setTokensUsed(domain.getTokensUsed());
        entity.setLearnedImprovement(domain.getLearnedImprovement());
        entity.setCategory(domain.getCategory());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setIsReviewed(domain.getIsReviewed());
        return entity;
    }
}
