package com.yourcompany.langchain4j.knowledge;

import com.yourcompany.langchain4j.tool.CodeQualityTool;
import com.yourcompany.langchain4j.tool.ProjectContextTool;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目管理增强器
 * 将代码生成优化功能集成到多项目管理体系
 */
@Slf4j
@Service
public class ProjectManagementEnhancer {
    
    private final MultiProjectKnowledgeManager multiProjectManager;
    private final CodeQualityTool codeQualityTool;
    private final ProjectContextTool projectContextTool;
    
    // 项目代码质量标准存储
    private final Map<String, ProjectCodeStandards> projectStandardsMap = new ConcurrentHashMap<>();
    
    // 项目代码示例库
    private final Map<String, List<KnowledgeDocument>> projectCodeExamplesMap = new ConcurrentHashMap<>();
    
    public ProjectManagementEnhancer(MultiProjectKnowledgeManager multiProjectManager,
                                     CodeQualityTool codeQualityTool,
                                     ProjectContextTool projectContextTool) {
        this.multiProjectManager = multiProjectManager;
        this.codeQualityTool = codeQualityTool;
        this.projectContextTool = projectContextTool;
    }
    
    /**
     * 初始化项目代码生成环境
     */
    public void initializeProjectEnvironment(String projectId, String projectPath) {
        log.info("初始化项目代码生成环境: {}", projectId);
        
        // 1. 分析项目结构
        String projectStructure = projectContextTool.analyzeProjectStructure(projectPath);
        log.info("项目结构分析完成: {}", projectId);
        
        // 2. 读取项目依赖
        String dependencies = projectContextTool.readDependencies(projectPath);
        log.info("项目依赖读取完成: {}", projectId);
        
        // 3. 获取编码规范
        String codingConventions = projectContextTool.getCodingConventions(projectPath);
        log.info("编码规范获取完成: {}", projectId);
        
        // 4. 保存项目标准
        ProjectCodeStandards standards = new ProjectCodeStandards();
        standards.setProjectId(projectId);
        standards.setProjectPath(projectPath);
        standards.setProjectStructure(projectStructure);
        standards.setDependencies(dependencies);
        standards.setCodingConventions(codingConventions);
        standards.setCreatedAt(LocalDateTime.now());
        
        projectStandardsMap.put(projectId, standards);
        
        // 5. 向项目知识库添加编码规范文档
        KnowledgeDocument conventionDoc = new KnowledgeDocument();
        conventionDoc.setId("conventions-" + projectId);
        conventionDoc.setTitle(projectId + " 项目编码规范");
        conventionDoc.setContent(codingConventions);
        conventionDoc.setCategory("编码规范");
        conventionDoc.setTags(new String[]{"coding-standards", "project:" + projectId});
        conventionDoc.setSource("项目分析");
        conventionDoc.setCreatedAt(LocalDateTime.now());
        conventionDoc.setAccessCount(0);
        
        multiProjectManager.addProjectDocument(projectId, conventionDoc);
        
        log.info("项目代码生成环境初始化完成: {}", projectId);
    }
    
    /**
     * 为项目添加代码示例到专属知识库
     */
    public void addCodeExampleToProject(String projectId, CodeExample example) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId("example-" + projectId + "-" + System.currentTimeMillis());
        doc.setTitle(example.getTitle());
        doc.setContent(example.getCode());
        doc.setCategory("代码示例");
        doc.setTags(example.getTags());
        doc.setSource("手动添加");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setAccessCount(0);
        
        multiProjectManager.addProjectDocument(projectId, doc);
        
        // 缓存到本地
        projectCodeExamplesMap.computeIfAbsent(projectId, k -> new ArrayList<>()).add(doc);
        
        log.info("向项目 {} 添加代码示例: {}", projectId, example.getTitle());
    }
    
    /**
     * 批量导入代码最佳实践到项目
     */
    public void importBestPractices(String projectId, List<CodeExample> examples) {
        log.info("向项目 {} 导入 {} 个最佳实践示例", projectId, examples.size());
        
        for (CodeExample example : examples) {
            addCodeExampleToProject(projectId, example);
        }
        
        log.info("最佳实践导入完成: {}", projectId);
    }
    
    /**
     * 验证项目代码质量
     */
    public ProjectQualityReport validateProjectCode(String projectId, String code) {
        ProjectCodeStandards standards = projectStandardsMap.get(projectId);
        if (standards == null) {
            throw new IllegalArgumentException("项目未初始化: " + projectId);
        }
        
        ProjectQualityReport report = new ProjectQualityReport();
        report.setProjectId(projectId);
        report.setValidatedAt(LocalDateTime.now());
        
        // 1. 基础质量验证
        String qualityResult = codeQualityTool.validateCodeQuality(code, "java");
        report.setQualityReport(qualityResult);
        report.setQualityScore(extractScore(qualityResult));
        
        // 2. 安全检查
        String securityResult = codeQualityTool.checkSecurityIssues(code);
        report.setSecurityReport(securityResult);
        
        // 3. 复杂度分析
        String complexityResult = codeQualityTool.analyzeComplexity(code);
        report.setComplexityReport(complexityResult);
        
        // 4. 规范检查
        String styleResult = codeQualityTool.checkCodeStyle(code, "java");
        report.setStyleReport(styleResult);
        
        // 5. 综合评分
        int overallScore = calculateOverallScore(report);
        report.setOverallScore(overallScore);
        report.setPassed(overallScore >= 80);
        
        log.info("项目 {} 代码质量验证完成，综合评分: {}/100", projectId, overallScore);
        
        return report;
    }
    
    /**
     * 查找项目内的相似代码示例
     */
    public List<String> findProjectCodeExamples(String projectId, String keyword) {
        String projectPath = projectStandardsMap.get(projectId).getProjectPath();
        return Collections.singletonList(
            projectContextTool.findSimilarCodeExamples(projectPath, keyword)
        );
    }
    
    /**
     * 获取项目代码生成上下文
     */
    public String getCodeGenerationContext(String projectId) {
        ProjectCodeStandards standards = projectStandardsMap.get(projectId);
        if (standards == null) {
            throw new IllegalArgumentException("项目未初始化: " + projectId);
        }
        
        StringBuilder context = new StringBuilder();
        context.append("【项目代码生成上下文】\n\n");
        context.append("项目ID: ").append(projectId).append("\n\n");
        
        context.append("## 项目结构\n");
        context.append(standards.getProjectStructure()).append("\n\n");
        
        context.append("## 技术栈依赖\n");
        context.append(standards.getDependencies()).append("\n\n");
        
        context.append("## 编码规范\n");
        context.append(standards.getCodingConventions()).append("\n\n");
        
        context.append("## 可用代码示例\n");
        List<KnowledgeDocument> examples = projectCodeExamplesMap.getOrDefault(projectId, new ArrayList<>());
        context.append("已加载 ").append(examples.size()).append(" 个代码示例\n\n");
        
        examples.stream().limit(5).forEach(doc -> {
            context.append("- ").append(doc.getTitle()).append("\n");
        });
        
        return context.toString();
    }
    
    /**
     * 从质量报告提取分数
     */
    private int extractScore(String report) {
        try {
            String[] lines = report.split("\n");
            for (String line : lines) {
                if (line.contains("质量评分")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String scoreStr = parts[1].trim().split("/")[0].trim();
                        return Integer.parseInt(scoreStr);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取质量分数失败", e);
        }
        return 0;
    }
    
    /**
     * 计算综合评分
     */
    private int calculateOverallScore(ProjectQualityReport report) {
        int qualityScore = report.getQualityScore();
        
        // 安全检查扣分
        if (report.getSecurityReport().contains("🔴 高危")) {
            qualityScore -= 30;
        } else if (report.getSecurityReport().contains("🟡 中危")) {
            qualityScore -= 15;
        }
        
        // 规范检查扣分
        if (!report.getStyleReport().contains("✅")) {
            qualityScore -= 10;
        }
        
        return Math.max(0, qualityScore);
    }
    
    /**
     * 获取所有已注册项目的代码质量标准
     */
    public Map<String, ProjectCodeStandards> getAllProjectStandards() {
        return new HashMap<>(projectStandardsMap);
    }
    
    /**
     * 获取项目代码示例列表
     */
    public List<KnowledgeDocument> getProjectCodeExamples(String projectId) {
        return new ArrayList<>(projectCodeExamplesMap.getOrDefault(projectId, new ArrayList<>()));
    }
    
    /**
     * 项目代码质量标准
     */
    @Data
    public static class ProjectCodeStandards {
        private String projectId;
        private String projectPath;
        private String projectStructure;
        private String dependencies;
        private String codingConventions;
        private LocalDateTime createdAt;
    }
    
    /**
     * 代码示例
     */
    @Data
    public static class CodeExample {
        private String title;
        private String code;
        private String[] tags;
        private String description;
    }
    
    /**
     * 项目质量报告
     */
    @Data
    public static class ProjectQualityReport {
        private String projectId;
        private int qualityScore;
        private int overallScore;
        private boolean passed;
        private String qualityReport;
        private String securityReport;
        private String complexityReport;
        private String styleReport;
        private LocalDateTime validatedAt;
    }
}
