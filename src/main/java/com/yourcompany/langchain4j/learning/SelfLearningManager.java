package com.yourcompany.langchain4j.learning;

import com.yourcompany.langchain4j.knowledge.KnowledgeBaseManager;
import com.yourcompany.langchain4j.knowledge.KnowledgeDocument;
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
 */
@Slf4j
@Service
public class SelfLearningManager {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    private final SkillManager skillManager;
    
    // 学习经验存储
    private final Map<String, LearningExperience> experienceStore = new ConcurrentHashMap<>();
    
    // 改进模式库（从经验中提取的改进模式）
    private final List<String> improvementPatterns = Collections.synchronizedList(new ArrayList<>());
    
    public SelfLearningManager(KnowledgeBaseManager knowledgeBaseManager, 
                               SkillManager skillManager) {
        this.knowledgeBaseManager = knowledgeBaseManager;
        this.skillManager = skillManager;
    }
    
    @PostConstruct
    public void initializeLearningSystem() {
        log.info("自主学习系统初始化完成");
    }
    
    /**
     * 记录交互经验
     */
    public String recordExperience(LearningExperience experience) {
        if (experience.getId() == null) {
            experience.setId(UUID.randomUUID().toString());
        }
        experience.setCreatedAt(LocalDateTime.now());
        experience.setIsReviewed(false);
        
        experienceStore.put(experience.getId(), experience);
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
            knowledge.setTitle(String.format("最佳实践: %s", experience.getQuery()));
            knowledge.setContent(experience.getLearnedImprovement());
            knowledge.setCategory(experience.getCategory() != null ? experience.getCategory() : "最佳实践");
            knowledge.setTags(experience.getUsedSkills());
            knowledge.setSource("自主学习");
            
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
        doc.setTitle(String.format("手动学习: %s", 
            knowledge.length() > 50 ? knowledge.substring(0, 50) + "..." : knowledge));
        doc.setContent(knowledge);
        doc.setCategory(category);
        doc.setSource("手动输入");
        doc.setCreatedAt(LocalDateTime.now());
        
        String docId = knowledgeBaseManager.addDocument(doc);
        log.info("手动学习新知识: {} - 分类: {}", docId, category);
        
        return docId;
    }
    
    /**
     * 审核学习经验
     */
    public boolean reviewExperience(String experienceId, boolean approved) {
        LearningExperience experience = experienceStore.get(experienceId);
        if (experience != null) {
            experience.setIsReviewed(approved);
            log.info("学习经验已审核: {} - 批准: {}", experienceId, approved);
            return true;
        }
        return false;
    }
    
    /**
     * 导出学习报告
     */
    public String generateLearningReport() {
        Map<String, Object> stats = getLearningStatistics();
        
        StringBuilder report = new StringBuilder();
        report.append("=== 自主学习报告 ===\n\n");
        report.append(String.format("总经验数: %s\n", stats.get("totalExperiences")));
        report.append(String.format("平均反馈分数: %s/5.0\n", stats.get("averageFeedbackScore")));
        report.append(String.format("正面经验: %s\n", stats.get("positiveExperiences")));
        report.append(String.format("负面经验: %s\n", stats.get("negativeExperiences")));
        report.append(String.format("改进模式: %s\n\n", stats.get("improvementPatterns")));
        
        if (!improvementPatterns.isEmpty()) {
            report.append("最近改进模式:\n");
            improvementPatterns.stream()
                .skip(Math.max(0, improvementPatterns.size() - 5))
                .forEach(pattern -> report.append("- ").append(pattern).append("\n"));
        }
        
        return report.toString();
    }
}
