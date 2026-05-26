package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.knowledge.*;
import com.yourcompany.langchain4j.service.ProjectAwareCodeGenerationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目管理 API 控制器
 * 提供项目注册、代码生成、质量验证等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/project-management")
@RequiredArgsConstructor
public class ProjectManagementController {
    
    private final MultiProjectKnowledgeManager multiProjectManager;
    private final ProjectManagementEnhancer projectEnhancer;
    private final ProjectAwareCodeGenerationService codeGenerationService;
    
    /**
     * 注册新项目
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerProject(@RequestBody ProjectRegisterRequest request) {
        log.info("注册新项目: {}", request.getProjectId());
        
        // 1. 创建项目配置
        MultiProjectKnowledgeManager.ProjectConfig config = new MultiProjectKnowledgeManager.ProjectConfig();
        config.setProjectId(request.getProjectId());
        config.setProjectName(request.getProjectName());
        config.setTechStack(request.getTechStack());
        config.setDomain(request.getDomain());
        config.setEnableGlobalSharing(request.isEnableGlobalSharing());
        
        // 2. 注册项目
        multiProjectManager.registerProject(config);
        
        // 3. 初始化项目代码生成环境
        if (request.getProjectPath() != null && !request.getProjectPath().isEmpty()) {
            projectEnhancer.initializeProjectEnvironment(
                request.getProjectId(), 
                request.getProjectPath()
            );
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "项目注册成功",
            "projectId", request.getProjectId()
        ));
    }
    
    /**
     * 获取所有项目列表
     */
    @GetMapping("/projects")
    public ResponseEntity<Map<String, Object>> getAllProjects() {
        List<MultiProjectKnowledgeManager.ProjectMetadata> projects = 
            multiProjectManager.getAllProjects();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "projects", projects,
            "count", projects.size()
        ));
    }
    
    /**
     * 获取项目详情
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectDetails(
            @PathVariable String projectId) {
        Map<String, Object> stats = multiProjectManager.getProjectStats(projectId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "project", stats
        ));
    }
    
    /**
     * 为项目生成代码
     */
    @PostMapping("/projects/{projectId}/generate-code")
    public ResponseEntity<Map<String, Object>> generateCode(
            @PathVariable String projectId,
            @RequestBody CodeGenerationRequest request) {
        log.info("为项目 {} 生成代码: {}", projectId, request.getRequirement());
        
        try {
            ProjectAwareCodeGenerationService.CodeGenerationResult result = 
                codeGenerationService.generateCodeForProject(projectId, request.getRequirement());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "code", result.getGeneratedCode(),
                "qualityScore", result.getQualityReport().getOverallScore(),
                "passed", result.getQualityReport().isPassed(),
                "usedExamples", result.getUsedExamples(),
                "qualityReport", result.getQualityReport()
            ));
            
        } catch (Exception e) {
            log.error("代码生成失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 优化项目现有代码
     */
    @PostMapping("/projects/{projectId}/optimize-code")
    public ResponseEntity<Map<String, Object>> optimizeCode(
            @PathVariable String projectId,
            @RequestBody CodeOptimizationRequest request) {
        log.info("为项目 {} 优化代码", projectId);
        
        try {
            ProjectAwareCodeGenerationService.CodeGenerationResult result = 
                codeGenerationService.optimizeCodeForProject(
                    projectId, 
                    request.getExistingCode(), 
                    request.getOptimizationGoal()
                );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "optimizedCode", result.getGeneratedCode(),
                "originalScore", result.getOriginalScore(),
                "optimizedScore", result.getOptimizedScore(),
                "improvement", result.getImprovement(),
                "qualityReport", result.getQualityReport()
            ));
            
        } catch (Exception e) {
            log.error("代码优化失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 验证代码质量
     */
    @PostMapping("/projects/{projectId}/validate-code")
    public ResponseEntity<Map<String, Object>> validateCode(
            @PathVariable String projectId,
            @RequestBody CodeValidationRequest request) {
        log.info("验证项目 {} 的代码质量", projectId);
        
        try {
            ProjectManagementEnhancer.ProjectQualityReport report = 
                projectEnhancer.validateProjectCode(projectId, request.getCode());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "report", report,
                "passed", report.isPassed()
            ));
            
        } catch (Exception e) {
            log.error("代码验证失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 向项目添加代码示例
     */
    @PostMapping("/projects/{projectId}/code-examples")
    public ResponseEntity<Map<String, Object>> addCodeExample(
            @PathVariable String projectId,
            @RequestBody CodeExampleRequest request) {
        log.info("向项目 {} 添加代码示例: {}", projectId, request.getTitle());
        
        ProjectManagementEnhancer.CodeExample example = new ProjectManagementEnhancer.CodeExample();
        example.setTitle(request.getTitle());
        example.setCode(request.getCode());
        example.setTags(request.getTags());
        example.setDescription(request.getDescription());
        
        projectEnhancer.addCodeExampleToProject(projectId, example);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "代码示例已添加"
        ));
    }
    
    /**
     * 获取项目代码示例
     */
    @GetMapping("/projects/{projectId}/code-examples")
    public ResponseEntity<Map<String, Object>> getCodeExamples(
            @PathVariable String projectId) {
        List<KnowledgeDocument> examples = 
            projectEnhancer.getProjectCodeExamples(projectId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "examples", examples,
            "count", examples.size()
        ));
    }
    
    /**
     * 获取项目代码生成上下文
     */
    @GetMapping("/projects/{projectId}/context")
    public ResponseEntity<Map<String, Object>> getGenerationContext(
            @PathVariable String projectId) {
        String context = projectEnhancer.getCodeGenerationContext(projectId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "context", context
        ));
    }
    
    /**
     * 搜索项目知识库
     */
    @GetMapping("/projects/{projectId}/search")
    public ResponseEntity<Map<String, Object>> searchKnowledge(
            @PathVariable String projectId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int maxResults) {
        List<SearchResult> results = 
            multiProjectManager.searchKnowledge(projectId, query, maxResults);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "results", results,
            "count", results.size()
        ));
    }
    
    /**
     * 跨项目检索
     */
    @PostMapping("/search-across-projects")
    public ResponseEntity<Map<String, Object>> searchAcrossProjects(
            @RequestBody CrossProjectSearchRequest request) {
        List<MultiProjectKnowledgeManager.CrossProjectResult> results = 
            multiProjectManager.searchAcrossProjects(
                request.getQuery(), 
                request.getProjectIds(), 
                request.getMaxResults()
            );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "results", results,
            "count", results.size()
        ));
    }
    
    // 请求 DTO 类
    
    @Data
    public static class ProjectRegisterRequest {
        private String projectId;
        private String projectName;
        private String projectPath;
        private String techStack;
        private String domain;
        private boolean enableGlobalSharing;
    }
    
    @Data
    public static class CodeGenerationRequest {
        private String requirement;
    }
    
    @Data
    public static class CodeOptimizationRequest {
        private String existingCode;
        private String optimizationGoal;
    }
    
    @Data
    public static class CodeValidationRequest {
        private String code;
    }
    
    @Data
    public static class CodeExampleRequest {
        private String title;
        private String code;
        private String[] tags;
        private String description;
    }
    
    @Data
    public static class CrossProjectSearchRequest {
        private String query;
        private List<String> projectIds;
        private int maxResults;
    }
}
