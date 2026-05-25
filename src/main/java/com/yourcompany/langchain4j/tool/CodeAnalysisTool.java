package com.yourcompany.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 工具集成 - 代码分析工具
 * 提供代码统计、复杂度分析等能力
 */
@Component
public class CodeAnalysisTool {

    @Tool("分析代码文件的基本统计信息（行数、注释行数等）")
    public Map<String, Object> analyzeCodeStats(String code) {
        Map<String, Object> stats = new HashMap<>();
        String[] lines = code.split("\n");
        
        int totalLines = lines.length;
        int commentLines = 0;
        int blankLines = 0;
        int codeLines = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                blankLines++;
            } else if (trimmed.startsWith("//") || trimmed.startsWith("/*") || 
                       trimmed.startsWith("*") || trimmed.startsWith("*/")) {
                commentLines++;
            } else {
                codeLines++;
            }
        }
        
        stats.put("totalLines", totalLines);
        stats.put("codeLines", codeLines);
        stats.put("commentLines", commentLines);
        stats.put("blankLines", blankLines);
        stats.put("commentRatio", String.format("%.2f%%", (commentLines * 100.0 / totalLines)));
        
        return stats;
    }

    @Tool("检测代码中的 TODO 和 FIXME 标记")
    public String findTodoAndFixme(String code) {
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("TODO") || line.contains("FIXME")) {
                result.append(String.format("Line %d: %s\n", i + 1, line.trim()));
            }
        }
        
        return result.length() > 0 ? result.toString() : "未发现 TODO 或 FIXME 标记";
    }

    @Tool("统计代码中方法的复杂度（基于参数数量和嵌套层级）")
    public String estimateMethodComplexity(String code) {
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        
        int braceDepth = 0;
        int methodCount = 0;
        int maxDepth = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            for (char c : trimmed.toCharArray()) {
                if (c == '{') {
                    braceDepth++;
                    maxDepth = Math.max(maxDepth, braceDepth);
                } else if (c == '}') {
                    braceDepth--;
                }
            }
            
            if (trimmed.matches(".*\\(.*\\).*\\{")) {
                methodCount++;
            }
        }
        
        result.append(String.format("方法数量: %d\n", methodCount));
        result.append(String.format("最大嵌套深度: %d\n", maxDepth));
        result.append(String.format("复杂度评估: %s\n", 
            maxDepth > 5 ? "高（建议重构）" : maxDepth > 3 ? "中" : "低"));
        
        return result.toString();
    }
}
