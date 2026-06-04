package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.agent.AgentOrchestrator;
import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.agent.OrchestrationResult;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final AgentOrchestrator agentOrchestrator;
    private final KnowledgeBaseManager knowledgeBaseManager;
    private final SkillManager skillManager;
    private final SelfLearningManager selfLearningManager;
    private final AiSupervisor aiSupervisor;
    
    /**
     * 智能编排入口（统一调度所有子 Agent）
     * 自动识别意图 → 选择流水线 → 多步骤执行 → 聚合结果
     * 
     * 支持的意图：
     * - code_generation: [代码生成 → 代码审查]
     * - code_review: [代码审查]
     * - analysis: [技术分析]
     * - documentation: [文档生成]
     * - complex_task: [项目分析 → 代码实现 → 代码审查 → 文档生成]
     * - general: [智能问答]
     */
    @PostMapping("/orchestrate")
    public ResponseEntity<Map<String, Object>> orchestrate(@RequestBody OrchestrateRequest request) {
        log.info("收到编排请求: message长度={}, hasCodeContext={}",
                request.getMessage() != null ? request.getMessage().length() : 0,
                request.getCodeContext() != null && !request.getCodeContext().isBlank());
        
        OrchestrationResult result = agentOrchestrator.orchestrate(
                request.getMessage(),
                request.getCodeContext()
        );
        
        // 记录 Token 使用（估算）
        if (result.isSuccess()) {
            TokenUsageRecord record = new TokenUsageRecord();
            record.setAgentName("AgentOrchestrator");
            record.setUserId(request.getUserId());
            record.setRequestType("orchestrate:" + result.getIntent());
            int inputEstimate = (request.getMessage() != null ? request.getMessage().length() : 0) / 4;
            int outputEstimate = (result.getFinalResult() != null ? result.getFinalResult().length() : 0) / 4;
            record.setInputTokens(inputEstimate);
            record.setOutputTokens(outputEstimate);
            record.setTotalTokens(inputEstimate + outputEstimate);
            record.setDurationMs(result.getTotalDurationMs());
            aiSupervisor.recordTokenUsage(record);
        }
        
        if (result.isSuccess()) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("intent", result.getIntent());
            response.put("intentLabel", result.getIntentLabel());
            response.put("confidence", result.getConfidence());
            response.put("pipeline", result.getPipeline());
            response.put("steps", result.getSteps());
            response.put("finalResult", result.getFinalResult() != null ? result.getFinalResult() : "");
            response.put("qualityScore", result.getQualityScore());
            response.put("fixRounds", result.getFixRounds());
            response.put("totalDurationMs", result.getTotalDurationMs());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", result.getErrorMessage() != null ? result.getErrorMessage() : "编排执行失败");
            errorResponse.put("intent", result.getIntent());
            errorResponse.put("intentLabel", result.getIntentLabel());
            errorResponse.put("steps", result.getSteps());
            errorResponse.put("totalDurationMs", result.getTotalDurationMs());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
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
     * 知识库 - 获取所有文档列表
     */
    @GetMapping("/knowledge/documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        List<KnowledgeDocument> docs = knowledgeBaseManager.getAllDocuments();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "documents", docs,
            "total", docs.size()
        ));
    }
    
    /**
     * 知识库 - 获取文档详情
     */
    @GetMapping("/knowledge/documents/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable String id) {
        return knowledgeBaseManager.getDocumentById(id)
            .map(doc -> ResponseEntity.ok(Map.of(
                "success", true,
                "document", doc
            )))
            .orElse(ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "文档不存在: " + id
            )));
    }
    
    /**
     * 知识库 - 删除文档
     */
    @DeleteMapping("/knowledge/documents/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String id) {
        log.info("删除知识文档: {}", id);
        boolean deleted = knowledgeBaseManager.deleteDocument(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "文档已删除"));
        }
        return ResponseEntity.badRequest().body(Map.of(
            "success", false, "error", "文档不存在或无法删除: " + id
        ));
    }
    
    /**
     * 知识库 - 下载文档内容
     */
    @GetMapping("/knowledge/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String id) {
        return knowledgeBaseManager.getDocumentById(id)
            .map(doc -> {
                String content = doc.getContent();
                String filename = (doc.getTitle() != null ? doc.getTitle() : doc.getId()) + ".md";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.setContentDispositionFormData("attachment", 
                    java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8));
                return new ResponseEntity<>(content.getBytes(java.nio.charset.StandardCharsets.UTF_8), 
                    headers, org.springframework.http.HttpStatus.OK);
            })
            .orElse(ResponseEntity.notFound().build());
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
        Map<String, Object> report = selfLearningManager.generateLearningReport();
        
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
    public static class OrchestrateRequest {
        private String userId;
        private String message;
        private String codeContext;
    }
    
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
