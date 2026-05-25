package com.yourcompany.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * IDE 集成工具 - 方案3：通过 LSP (Language Server Protocol) 或 IDE API
 * 适用于：VS Code、IntelliJ IDEA、Eclipse 等 IDE 插件
 * 
 * 注：这是示例实现，实际需要根据具体 IDE 的 API 进行适配
 */
@Slf4j
@Component
public class IDEIntegrationTool {
    
    @Tool("在IDE中打开文件并显示代码差异（Diff View）")
    public String openDiffView(
            @ToolMemory String filePath,
            @ToolMemory String newCode,
            @ToolMemory String description) {
        
        // 实际实现需要调用 IDE 的 API
        // 例如 VS Code: vscode.commands.executeCommand('vscode.diff', ...)
        
        return String.format("""
            📝 IDE Diff View 已打开
            
            文件: %s
            描述: %s
            
            💡 下一步操作：
            1. 查看代码差异
            2. 确认修改
            3. 点击 'Accept' 应用更改
            
            ⚠️ 注意：需要安装对应的 IDE 插件才能使用此功能
            """, filePath, description);
    }
    
    @Tool("在 IDE 终端中执行代码格式化命令")
    public String formatCode(@ToolMemory String filePath, @ToolMemory String formatter) {
        
        String command = switch (formatter.toLowerCase()) {
            case "prettier" -> "npx prettier --write " + filePath;
            case "eslint" -> "npx eslint --fix " + filePath;
            case "checkstyle" -> "mvn checkstyle:checkstyle";
            case "black" -> "black " + filePath;
            default -> "未知格式化工具: " + formatter;
        };
        
        return String.format("""
            🎨 代码格式化
            
            文件: %s
            工具: %s
            命令: %s
            
            ✅ 格式化完成
            """, filePath, formatter, command);
    }
    
    @Tool("运行单元测试并查看结果")
    public String runUnitTests(@ToolMemory String testClass, @ToolMemory String testMethod) {
        
        String command;
        
        if (testMethod != null && !testMethod.isEmpty()) {
            command = String.format("mvn test -Dtest=%s#%s", testClass, testMethod);
        } else {
            command = String.format("mvn test -Dtest=%s", testClass);
        }
        
        return String.format("""
            🧪 运行单元测试
            
            测试类: %s
            测试方法: %s
            命令: %s
            
            💡 实际执行需要集成 IDE 或 CI 系统
            """, testClass, testMethod, command);
    }
    
    @Tool("在 IDE 中显示代码建议面板")
    public String showCodeSuggestion(
            @ToolMemory String suggestion,
            @ToolMemory String codeSnippet,
            @ToolMemory String filePath) {
        
        return String.format("""
            💡 AI 代码建议
            
            文件: %s
            
            建议:
            %s
            
            代码示例:
            ```
            %s
            ```
            
            ✅ 点击 'Apply' 应用建议
            ❌ 点击 'Reject' 拒绝建议
            """, filePath, suggestion, codeSnippet);
    }
    
    @Tool("配置 IDE 的 AI 助手插件")
    public String configureIDEPlugin(
            @ToolMemory String ideType,
            @ToolMemory String apiKey,
            @ToolMemory String model) {
        
        String configTemplate = switch (ideType.toLowerCase()) {
            case "vscode" -> """
                // VS Code settings.json
                {
                  "ai-assistant.apiKey": "%s",
                  "ai-assistant.model": "%s",
                  "ai-assistant.enableAutoComplete": true,
                  "ai-assistant.enableChat": true
                }
                """.formatted(apiKey, model);
                
            case "intellij" -> """
                // IntelliJ IDEA 配置
                Settings -> Tools -> AI Assistant
                - API Key: %s
                - Model: %s
                - Enable Code Completion: ✓
                - Enable Chat Panel: ✓
                """.formatted(apiKey, model);
                
            default -> "不支持的 IDE 类型: " + ideType;
        };
        
        return String.format("""
            ⚙️ IDE 插件配置
            
            IDE 类型: %s
            
            配置内容:
            %s
            
            💡 重启 IDE 后生效
            """, ideType, configTemplate);
    }
}
