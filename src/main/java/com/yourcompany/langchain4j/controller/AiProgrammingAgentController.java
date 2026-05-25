package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.knowledge.KnowledgeBaseManager;
import com.yourcompany.langchain4j.knowledge.KnowledgeDocument;
import com.yourcompany.langchain4j.learning.LearningExperience;
import com.yourcompany.langchain4j.learning.SelfLearningManager;
import com.yourcompany.langchain4j.skill.Skill;
import com.yourcompany.langchain4j.skill.SkillManager;
import com.yourcompany.langchain4j.supervisor.AiSupervisor;
import com.yourcompany.langchain4j.supervisor.TokenUsageRecord;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 编程 Agent API 控制器
 * 提供编程助手、知识库、技能管理和监管功能
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-programming-agent")
@RequiredArgsConstructor
public class AiProgrammingAgentController {
    
    private final AiProgrammingAgent aiProgrammingAgent;
    private final KnowledgeBaseManager knowledgeBaseManager;
    private final SkillManager skillManager;
    private final SelfLearningManager selfLearningManager;
    private final AiSupervisor aiSupervisor;
    
    /**
     * 执行编程任务
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeTask(@RequestBody ExecuteTaskRequest request) {
        log.info("收到编程任务: {}", request.getTask());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 监管检查
            aiSupervisor.incrementConcurrentRequests();
            
            // 执行任务
            String result = aiProgrammingAgent.executeTask(
                request.getTask(), 
                request.getContext()
            );
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录 Token 使用（估算）
            TokenUsageRecord record = new TokenUsageRecord();
            record.setAgentName("AiProgrammingAgent");
            record.setUserId(request.getUserId());
            record.setRequestType("execute");
            record.setInputTokens(request.getTask().length() / 4);
            record.setOutputTokens(result.length() / 4);
            record.setTotalTokens((request.getTask().length() + result.length()) / 4);
            record.setDurationMs(duration);
            
            aiSupervisor.recordTokenUsage(record);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "result", result,
                "duration", duration + "ms"
            ));
            
        } catch (Exception e) {
            log.error("执行编程任务失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } finally {
            aiSupervisor.decrementConcurrentRequests();
        }
    }
    
    /**
     * 代码审查
     */
    @PostMapping("/review-code")
    public ResponseEntity<Map<String, Object>> reviewCode(@RequestBody CodeReviewRequest request) {
        log.info("收到代码审查请求: {}", request.getLanguage());
        
        String result = aiProgrammingAgent.reviewCode(
            request.getCode(), 
            request.getLanguage()
        );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "review", result
        ));
    }
    
    /**
     * 技术问题解答（带知识库检索）
     */
    @PostMapping("/answer-question")
    public ResponseEntity<Map<String, Object>> answerQuestion(@RequestBody QuestionRequest request) {
        log.info("收到技术问题: {}", request.getQuestion());
        
        String result = aiProgrammingAgent.answerTechnicalQuestion(request.getQuestion());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "answer", result
        ));
    }
    
    /**
     * 生成文档
     */
    @PostMapping("/generate-doc")
    public ResponseEntity<Map<String, Object>> generateDocumentation(
            @RequestBody GenerateDocRequest request) {
        log.info("收到文档生成请求: {}", request.getDocType());
        
        String result = aiProgrammingAgent.generateDocumentation(
            request.getProjectInfo(), 
            request.getDocType()
        );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "documentation", result
        ));
    }
    
    /**
     * 知识库 - 添加文档
     */
    @PostMapping("/knowledge/add")
    public ResponseEntity<Map<String, Object>> addKnowledge(
            @RequestBody KnowledgeDocument document) {
        log.info("添加知识文档: {}", document.getTitle());
        
        String docId = knowledgeBaseManager.addDocument(document);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "documentId", docId
        ));
    }
    
    /**
     * 知识库 - 检索文档
     */
    @GetMapping("/knowledge/search")
    public ResponseEntity<Map<String, Object>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int maxResults) {
        log.info("检索知识库: {}", query);
        
        List<KnowledgeDocument> results = 
            knowledgeBaseManager.searchRelevantDocuments(query, maxResults, 0.6);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "results", results,
            "count", results.size()
        ));
    }
    
    /**
     * 知识库 - 获取统计信息
     */
    @GetMapping("/knowledge/stats")
    public ResponseEntity<Map<String, Object>> getKnowledgeStats() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "statistics", knowledgeBaseManager.getKnowledgeBaseStats()
        ));
    }
    
    /**
     * 技能 - 搜索技能
     */
    @GetMapping("/skills/search")
    public ResponseEntity<Map<String, Object>> searchSkills(
            @RequestParam String query) {
        List<Skill> skills = skillManager.searchSkills(query);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "skills", skills,
            "count", skills.size()
        ));
    }
    
    /**
     * 技能 - 获取所有技能
     */
    @GetMapping("/skills/all")
    public ResponseEntity<Map<String, Object>> getAllSkills() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "skills", skillManager.getAllSkills()
        ));
    }
    
    /**
     * 技能 - 添加新技能
     */
    @PostMapping("/skills/add")
    public ResponseEntity<Map<String, Object>> addSkill(@RequestBody Skill skill) {
        skillManager.registerSkill(skill);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "技能已添加"
        ));
    }
    
    /**
     * 学习 - 记录经验反馈
     */
    @PostMapping("/learning/feedback")
    public ResponseEntity<Map<String, Object>> recordFeedback(
            @RequestBody LearningExperience experience) {
        String experienceId = selfLearningManager.recordExperience(experience);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "experienceId", experienceId
        ));
    }
    
    /**
     * 学习 - 手动学习新知识
     */
    @PostMapping("/learning/learn")
    public ResponseEntity<Map<String, Object>> learnKnowledge(
            @RequestParam String knowledge,
            @RequestParam String category) {
        String docId = selfLearningManager.learnNewKnowledge(knowledge, category);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "documentId", docId
        ));
    }
    
    /**
     * 学习 - 获取学习报告
     */
    @GetMapping("/learning/report")
    public ResponseEntity<Map<String, Object>> getLearningReport() {
        String report = selfLearningManager.generateLearningReport();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "report", report,
            "statistics", selfLearningManager.getLearningStatistics()
        ));
    }
    
    /**
     * 监管 - 获取 Token 使用统计
     */
    @GetMapping("/supervisor/stats")
    public ResponseEntity<Map<String, Object>> getSupervisorStats() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "statistics", aiSupervisor.getTokenUsageStats()
        ));
    }
    
    /**
     * 监管 - 获取监管报告
     */
    @GetMapping("/supervisor/report")
    public ResponseEntity<Map<String, Object>> getSupervisorReport() {
        String report = aiSupervisor.generateSupervisionReport();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "report", report
        ));
    }
    
    /**
     * 监管 - 获取警报日志
     */
    @GetMapping("/supervisor/alerts")
    public ResponseEntity<Map<String, Object>> getAlertLogs(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "alerts", aiSupervisor.getAlertLogs(limit)
        ));
    }
    
    // 请求 DTO 类
    
    @Data
    public static class ExecuteTaskRequest {
        private String userId;
        private String task;
        private String context;
    }
    
    @Data
    public static class CodeReviewRequest {
        private String code;
        private String language;
    }
    
    @Data
    public static class QuestionRequest {
        private String question;
    }
    
    @Data
    public static class GenerateDocRequest {
        private String projectInfo;
        private String docType;
    }
}
