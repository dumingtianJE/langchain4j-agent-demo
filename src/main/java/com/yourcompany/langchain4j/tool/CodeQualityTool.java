package com.yourcompany.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 代码质量验证工具
 * 在代码生成后自动验证代码质量和准确性
 */
@Slf4j
@Component
public class CodeQualityTool {
    
    /**
     * 验证生成的代码是否包含必要的元素
     */
    @Tool("验证代码质量：检查是否包含 import、注释、错误处理等必要元素")
    public String validateCodeQuality(String code, String language) {
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        int score = 100;
        
        if ("java".equalsIgnoreCase(language)) {
            // 检查 import 语句
            if (!code.contains("import ") && code.contains("class ")) {
                issues.add("缺少 import 语句");
                score -= 15;
            }
            
            // 检查类注释
            if (code.contains("class ") && !code.contains("/**")) {
                issues.add("缺少类级别的 JavaDoc 注释");
                score -= 10;
            }
            
            // 检查方法注释
            if (code.contains("public ") && !code.contains("* @")) {
                suggestions.add("建议添加方法级别的 JavaDoc 注释");
                score -= 5;
            }
            
            // 检查异常处理
            if (code.contains("throws ") || code.contains("try ")) {
                suggestions.add("包含异常处理 - 良好实践");
            } else if (code.contains("InputStream") || code.contains("Reader") 
                    || code.contains("Database") || code.contains("HTTP")) {
                issues.add("IO/网络操作缺少异常处理");
                score -= 15;
            }
            
            // 检查日志
            if (!code.contains("log.") && !code.contains("logger.") 
                && !code.contains("System.out") && code.contains("class ")) {
                suggestions.add("建议添加日志记录");
                score -= 5;
            }
            
            // 检查魔法数字
            if (code.matches(".*[^\\d]\\d{2,}[^\\d].*")) {
                suggestions.add("存在魔法数字，建议定义为常量");
                score -= 5;
            }
            
            // 检查空指针防护
            if (code.contains(".toString()") || code.contains(".length()") 
                || code.contains(".equals(")) {
                if (!code.contains("!= null") && !code.contains("== null")
                    && !code.contains("Optional")) {
                    suggestions.add("建议添加空指针防护");
                    score -= 10;
                }
            }
        }
        
        // 生成验证报告
        StringBuilder report = new StringBuilder();
        report.append("【代码质量验证报告】\n\n");
        report.append("质量评分: ").append(Math.max(0, score)).append("/100\n\n");
        
        if (!issues.isEmpty()) {
            report.append("❌ 发现的问题:\n");
            for (String issue : issues) {
                report.append("  - ").append(issue).append("\n");
            }
            report.append("\n");
        }
        
        if (!suggestions.isEmpty()) {
            report.append("💡 改进建议:\n");
            for (String suggestion : suggestions) {
                report.append("  - ").append(suggestion).append("\n");
            }
            report.append("\n");
        }
        
        if (score >= 90) {
            report.append("✅ 代码质量优秀，可以直接使用");
        } else if (score >= 70) {
            report.append("⚠️ 代码质量良好，建议根据提示优化");
        } else {
            report.append("⚠️ 代码需要改进，请根据问题列表修改");
        }
        
        return report.toString();
    }
    
    /**
     * 检查代码安全性
     */
    @Tool("检查代码安全漏洞：SQL注入、XSS、硬编码密码等")
    public String checkSecurityIssues(String code) {
        List<String> vulnerabilities = new ArrayList<>();
        
        // SQL 注入检查
        if (code.contains("Statement") && !code.contains("PreparedStatement")) {
            vulnerabilities.add("🔴 高危: 使用 Statement 而非 PreparedStatement，存在 SQL 注入风险");
        }
        
        // 硬编码密码检查
        if (code.matches(".*password.*=.*[\"'].*[\"'].*")) {
            vulnerabilities.add("🔴 高危: 代码中包含硬编码密码，应使用配置管理");
        }
        
        // 硬编码 API Key
        if (code.matches(".*(api[_-]?key|apikey|token).*:.*[\"'].{20,}[\"'].*")) {
            vulnerabilities.add("🔴 高危: 代码中包含硬编码 API Key，应使用环境变量");
        }
        
        // XSS 风险
        if (code.contains("response.getWriter().write(") && !code.contains("escape")) {
            vulnerabilities.add("🟡 中危: 直接输出用户输入，存在 XSS 风险");
        }
        
        // 敏感信息日志
        if (code.contains("log.") && code.contains("password")) {
            vulnerabilities.add("🟡 中危: 日志可能包含敏感信息");
        }
        
        // 路径遍历
        if (code.contains("new File(") && !code.contains("normalize")) {
            vulnerabilities.add("🟡 中危: 文件操作缺少路径规范化，可能存在路径遍历攻击");
        }
        
        if (vulnerabilities.isEmpty()) {
            return "✅ 未发现明显安全漏洞";
        }
        
        StringBuilder report = new StringBuilder("【安全检查报告】\n\n");
        for (String vuln : vulnerabilities) {
            report.append(vuln).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * 代码复杂度分析
     */
    @Tool("分析代码复杂度：圈复杂度、方法长度、嵌套深度等")
    public String analyzeComplexity(String code) {
        String[] lines = code.split("\n");
        int totalLines = lines.length;
        int methodCount = 0;
        int maxNestingDepth = 0;
        int complexMethods = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // 统计方法数
            if (trimmed.matches(".*(public|private|protected).*\\(.*\\).*\\{")) {
                methodCount++;
            }
            
            // 估算嵌套深度
            long nestingLevel = line.chars().filter(ch -> ch == ' ').count() / 4;
            maxNestingDepth = Math.max(maxNestingDepth, (int) nestingLevel);
            
            // 检查复杂条件
            if (trimmed.contains(" && ") || trimmed.contains(" || ")) {
                complexMethods++;
            }
        }
        
        StringBuilder report = new StringBuilder("【代码复杂度分析】\n\n");
        report.append("总行数: ").append(totalLines).append(" 行\n");
        report.append("方法数: ").append(methodCount).append(" 个\n");
        report.append("最大嵌套深度: ").append(maxNestingDepth).append(" 层\n");
        report.append("复杂条件判断: ").append(complexMethods).append(" 处\n\n");
        
        // 评估
        List<String> warnings = new ArrayList<>();
        if (totalLines > 500) {
            warnings.add("⚠️ 文件过长，建议拆分为多个类");
        }
        if (maxNestingDepth > 3) {
            warnings.add("⚠️ 嵌套过深，建议使用卫语句或提取方法");
        }
        if (complexMethods > 5) {
            warnings.add("⚠️ 复杂条件较多，建议提取为独立方法");
        }
        
        if (warnings.isEmpty()) {
            report.append("✅ 代码复杂度合理");
        } else {
            report.append("改进建议:\n");
            warnings.forEach(w -> report.append("  ").append(w).append("\n"));
        }
        
        return report.toString();
    }
    
    /**
     * 检查代码规范
     */
    @Tool("检查代码是否符合命名规范和编码规范")
    public String checkCodeStyle(String code, String language) {
        List<String> violations = new ArrayList<>();
        
        if ("java".equalsIgnoreCase(language)) {
            // 类名应该大驼峰
            if (code.matches(".*class [a-z].*")) {
                violations.add("❌ 类名应使用大驼峰命名法（PascalCase）");
            }
            
            // 方法名应该小驼峰
            if (code.matches(".*(public|private|protected).*\\s+[A-Z].*\\(.*")) {
                violations.add("❌ 方法名应使用小驼峰命名法（camelCase）");
            }
            
            // 常量应该全大写
            if (code.matches(".*static final.*[a-z].*=.*")) {
                violations.add("❌ 常量应使用全大写下划线分隔（UPPER_SNAKE_CASE）");
            }
            
            // 检查单行长度
            String[] lines = code.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].length() > 120) {
                    violations.add("⚠️ 第 " + (i+1) + " 行超过 120 字符");
                    if (violations.size() > 5) break; // 最多报告5个
                }
            }
        }
        
        if (violations.isEmpty()) {
            return "✅ 代码符合命名规范和编码规范";
        }
        
        StringBuilder report = new StringBuilder("【代码规范检查】\n\n");
        violations.forEach(v -> report.append(v).append("\n"));
        return report.toString();
    }
    
    /**
     * 生成单元测试模板
     */
    @Tool("为给定代码生成单元测试模板")
    public String generateUnitTestTemplate(String sourceCode, String className) {
        StringBuilder testClass = new StringBuilder();
        testClass.append("import org.junit.jupiter.api.Test;\n");
        testClass.append("import org.junit.jupiter.api.BeforeEach;\n");
        testClass.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        testClass.append("class ").append(className).append("Test {\n\n");
        testClass.append("    private ").append(className).append(" target;\n\n");
        testClass.append("    @BeforeEach\n");
        testClass.append("    void setUp() {\n");
        testClass.append("        target = new ").append(className).append("();\n");
        testClass.append("    }\n\n");
        testClass.append("    @Test\n");
        testClass.append("    void should_When_GivenValidInput() {\n");
        testClass.append("        // Given\n");
        testClass.append("        // TODO: 准备测试数据\n\n");
        testClass.append("        // When\n");
        testClass.append("        // TODO: 执行被测试方法\n\n");
        testClass.append("        // Then\n");
        testClass.append("        // TODO: 验证结果\n");
        testClass.append("    }\n");
        testClass.append("}\n");
        
        return testClass.toString();
    }
}
