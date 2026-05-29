package com.yourcompany.langchain4j.service;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.knowledge.MultiProjectKnowledgeManager;
import com.yourcompany.langchain4j.knowledge.MultiProjectKnowledgeManager.SearchResult;
import com.yourcompany.langchain4j.knowledge.ProjectManagementEnhancer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目感知的代码生成服务
 * 根据项目上下文生成高质量代码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAwareCodeGenerationService {
    
    private final AiProgrammingAgent aiProgrammingAgent;
    private final MultiProjectKnowledgeManager multiProjectManager;
    private final ProjectManagementEnhancer projectEnhancer;
    
    /**
     * 为指定项目生成代码
     */
    public CodeGenerationResult generateCodeForProject(String projectId, String requirement) {
        log.info("为项目 {} 生成代码: {}", projectId, requirement);
        
        // 1. 获取项目上下文
        String projectContext = projectEnhancer.getCodeGenerationContext(projectId);
        
        // 2. 检索项目知识库中的相关示例
        List<SearchResult> relevantExamples = multiProjectManager.searchKnowledge(
            projectId, 
            extractKeywords(requirement), 
            3
        );
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append(projectContext).append("\n\n");
        
        if (!relevantExamples.isEmpty()) {
            contextBuilder.append("## 相关代码示例\n\n");
            for (SearchResult result : relevantExamples) {
                contextBuilder.append("### ").append(result.getDocument().getTitle()).append("\n");
                contextBuilder.append(result.getDocument().getContent()).append("\n\n");
            }
        }
        
        // 3. 构建增强Prompt
        String enhancedRequirement = buildEnhancedPrompt(requirement, contextBuilder.toString());
        
        // 4. 生成代码
        String generatedCode = aiProgrammingAgent.executeTask(enhancedRequirement, projectContext);
        
        // 5. 验证代码质量
        ProjectManagementEnhancer.ProjectQualityReport qualityReport = 
            projectEnhancer.validateProjectCode(projectId, extractCodeFromResponse(generatedCode));
        
        // 6. 如果质量不达标，要求重新生成
        if (!qualityReport.isPassed()) {
            log.warn("代码质量未达标 ({}/100)，要求重新生成", qualityReport.getOverallScore());
            
            String feedback = String.format("""
                代码质量评分：%d/100（未达到 80 分标准）
                
                质量问题：
                %s
                
                安全问题：
                %s
                
                请根据以上反馈重新优化代码。
                """,
                qualityReport.getOverallScore(),
                qualityReport.getQualityReport(),
                qualityReport.getSecurityReport()
            );
            
            generatedCode = aiProgrammingAgent.executeTask(
                requirement + "\n\n" + feedback, 
                projectContext
            );
            
            // 再次验证
            qualityReport = projectEnhancer.validateProjectCode(
                projectId, 
                extractCodeFromResponse(generatedCode)
            );
        }
        
        log.info("代码生成完成，质量评分: {}/100", qualityReport.getOverallScore());
        
        return CodeGenerationResult.builder()
            .projectId(projectId)
            .requirement(requirement)
            .generatedCode(generatedCode)
            .qualityReport(qualityReport)
            .usedExamples(relevantExamples.size())
            .build();
    }
    
    /**
     * 为项目优化现有代码
     */
    public CodeGenerationResult optimizeCodeForProject(String projectId, 
                                                       String existingCode, 
                                                       String optimizationGoal) {
        log.info("为项目 {} 优化代码: {}", projectId, optimizationGoal);
        
        // 1. 获取项目上下文
        String projectContext = projectEnhancer.getCodeGenerationContext(projectId);
        
        // 2. 分析现有代码质量
        ProjectManagementEnhancer.ProjectQualityReport currentQuality = 
            projectEnhancer.validateProjectCode(projectId, existingCode);
        
        // 3. 构建优化Prompt
        String optimizationPrompt = String.format("""
            请优化以下代码：%s
            
            当前代码质量问题：
            - 评分：%d/100
            - 问题报告：%s
            
            优化目标：%s
            
            请确保优化后的代码：
            1. 符合项目编码规范
            2. 解决所有安全问题
            3. 降低代码复杂度
            4. 提升性能和可维护性
            """,
            existingCode,
            currentQuality.getQualityScore(),
            currentQuality.getQualityReport(),
            optimizationGoal
        );
        
        // 4. 执行优化
        String optimizedCode = aiProgrammingAgent.executeTask(optimizationPrompt, projectContext);
        
        // 5. 验证优化结果
        ProjectManagementEnhancer.ProjectQualityReport optimizedQuality = 
            projectEnhancer.validateProjectCode(projectId, extractCodeFromResponse(optimizedCode));
        
        return CodeGenerationResult.builder()
            .projectId(projectId)
            .requirement(optimizationGoal)
            .generatedCode(optimizedCode)
            .qualityReport(optimizedQuality)
            .originalScore(currentQuality.getQualityScore())
            .optimizedScore(optimizedQuality.getOverallScore())
            .improvement(optimizedQuality.getOverallScore() - currentQuality.getQualityScore())
            .build();
    }
    
    /**
     * 构建增强Prompt
     */
    private String buildEnhancedPrompt(String requirement, String context) {
        return String.format("""
            请根据以下项目上下文生成代码：
            
            %s
            
            ---
            
            需求：
            %s
            
            请确保生成的代码：
            1. 完全符合项目编码规范
            2. 使用项目已有的技术栈和依赖
            3. 参考项目中的代码示例风格
            4. 包含完整的注释和文档
            5. 遵循最佳实践和设计模式
            """, context, requirement);
    }
    
    /**
     * 从需求中提取关键词
     */
    private String extractKeywords(String requirement) {
        // 简单实现：提取技术关键词
        StringBuilder keywords = new StringBuilder();
        
        String[] techKeywords = {
            "controller", "service", "repository", "entity", "dto",
            "rest", "api", "validation", "exception", "transaction",
            "jpa", "hibernate", "spring", "boot", "security"
        };
        
        String lowerReq = requirement.toLowerCase();
        for (String keyword : techKeywords) {
            if (lowerReq.contains(keyword)) {
                keywords.append(keyword).append(" ");
            }
        }
        
        return keywords.length() > 0 ? keywords.toString() : requirement;
    }
    
    /**
     * 从响应中提取代码
     */
    private String extractCodeFromResponse(String response) {
        // 简单实现：提取 markdown 代码块
        int startIndex = response.indexOf("```");
        if (startIndex == -1) {
            return response;
        }
        
        int firstLineEnd = response.indexOf("\n", startIndex);
        int endIndex = response.indexOf("```", firstLineEnd + 1);
        
        if (endIndex == -1) {
            return response.substring(firstLineEnd + 1);
        }
        
        return response.substring(firstLineEnd + 1, endIndex);
    }
    
    /**
     * 代码生成结果
     */
    public static class CodeGenerationResult {
        private String projectId;
        private String requirement;
        private String generatedCode;
        private ProjectManagementEnhancer.ProjectQualityReport qualityReport;
        private int usedExamples;
        private Integer originalScore;
        private Integer optimizedScore;
        private Integer improvement;
        
        public static CodeGenerationResultBuilder builder() {
            return new CodeGenerationResultBuilder();
        }
        
        public static class CodeGenerationResultBuilder {
            private String projectId;
            private String requirement;
            private String generatedCode;
            private ProjectManagementEnhancer.ProjectQualityReport qualityReport;
            private int usedExamples;
            private Integer originalScore;
            private Integer optimizedScore;
            private Integer improvement;
            
            public CodeGenerationResultBuilder projectId(String projectId) {
                this.projectId = projectId;
                return this;
            }
            
            public CodeGenerationResultBuilder requirement(String requirement) {
                this.requirement = requirement;
                return this;
            }
            
            public CodeGenerationResultBuilder generatedCode(String generatedCode) {
                this.generatedCode = generatedCode;
                return this;
            }
            
            public CodeGenerationResultBuilder qualityReport(ProjectManagementEnhancer.ProjectQualityReport qualityReport) {
                this.qualityReport = qualityReport;
                return this;
            }
            
            public CodeGenerationResultBuilder usedExamples(int usedExamples) {
                this.usedExamples = usedExamples;
                return this;
            }
            
            public CodeGenerationResultBuilder originalScore(Integer originalScore) {
                this.originalScore = originalScore;
                return this;
            }
            
            public CodeGenerationResultBuilder optimizedScore(Integer optimizedScore) {
                this.optimizedScore = optimizedScore;
                return this;
            }
            
            public CodeGenerationResultBuilder improvement(Integer improvement) {
                this.improvement = improvement;
                return this;
            }
            
            public CodeGenerationResult build() {
                CodeGenerationResult result = new CodeGenerationResult();
                result.projectId = this.projectId;
                result.requirement = this.requirement;
                result.generatedCode = this.generatedCode;
                result.qualityReport = this.qualityReport;
                result.usedExamples = this.usedExamples;
                result.originalScore = this.originalScore;
                result.optimizedScore = this.optimizedScore;
                result.improvement = this.improvement;
                return result;
            }
        }
        
        // Getters
        public String getProjectId() { return projectId; }
        public String getRequirement() { return requirement; }
        public String getGeneratedCode() { return generatedCode; }
        public ProjectManagementEnhancer.ProjectQualityReport getQualityReport() { return qualityReport; }
        public int getUsedExamples() { return usedExamples; }
        public Integer getOriginalScore() { return originalScore; }
        public Integer getOptimizedScore() { return optimizedScore; }
        public Integer getImprovement() { return improvement; }
    }
}
