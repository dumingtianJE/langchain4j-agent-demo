package com.yourcompany.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 代码文件写入工具 - 方案1：直接文件操作
 * 适用于：个人项目、快速原型、本地开发
 */
@Slf4j
@Component
public class CodeWriteTool {
    
    @Tool("将AI生成的代码写入指定文件路径。支持创建新文件和修改现有文件。参数：filePath-文件路径, codeContent-代码内容, description-描述")
    public String writeCodeToFile(String filePath, String codeContent, String description) {
        
        try {
            Path path = Paths.get(filePath);
            
            // 1. 安全检查
            if (!isPathSafe(path)) {
                return "❌ 错误：不允许写入该路径（安全限制）";
            }
            
            // 2. 备份现有文件（如果存在）
            if (Files.exists(path)) {
                backupFile(path);
            }
            
            // 3. 创建目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("创建目录: {}", parentDir);
            }
            
            // 4. 写入文件
            Files.writeString(path, codeContent, StandardCharsets.UTF_8);
            
            // 5. 记录操作日志
            logOperation(filePath, description, "WRITE");
            
            return String.format("✅ 成功写入文件: %s\n描述: %s\n大小: %d 字节",
                filePath, description, codeContent.length());
            
        } catch (IOException e) {
            log.error("写入文件失败: {}", filePath, e);
            return "❌ 写入文件失败: " + e.getMessage();
        }
    }
    
    @Tool("读取指定文件的内容，用于查看现有代码或验证写入结果。参数：filePath-文件路径")
    public String readFileContent(String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return "❌ 错误：文件不存在 - " + filePath;
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return String.format("✅ 文件内容 (%d 字符):\n\n%s", content.length(), content);
            
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return "❌ 读取文件失败: " + e.getMessage();
        }
    }
    
    @Tool("创建新文件，如果文件已存在则返回错误。参数：filePath-文件路径, codeContent-代码内容, description-描述")
    public String createNewFile(String filePath, String codeContent, String description) {
        
        Path path = Paths.get(filePath);
        
        if (Files.exists(path)) {
            return "❌ 错误：文件已存在，请使用 writeCodeToFile 覆盖";
        }
        
        return writeCodeToFile(filePath, codeContent, description);
    }
    
    @Tool("追加内容到现有文件末尾。参数：filePath-文件路径, codeContent-代码内容, description-描述")
    public String appendToFile(String filePath, String codeContent, String description) {
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return "❌ 错误：文件不存在，请先创建文件";
            }
            
            // 追加内容
            Files.writeString(path, codeContent, StandardCharsets.UTF_8, 
                java.nio.file.StandardOpenOption.APPEND);
            
            logOperation(filePath, description, "APPEND");
            
            return String.format("✅ 成功追加内容到: %s\n描述: %s", filePath, description);
            
        } catch (IOException e) {
            log.error("追加内容失败: {}", filePath, e);
            return "❌ 追加内容失败: " + e.getMessage();
        }
    }
    
    @Tool("删除指定文件（会先备份）。参数：filePath-文件路径, reason-删除原因")
    public String deleteFile(String filePath, String reason) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return "❌ 错误：文件不存在 - " + filePath;
            }
            
            // 先备份
            backupFile(path);
            
            // 删除文件
            Files.delete(path);
            
            logOperation(filePath, reason, "DELETE");
            
            return String.format("✅ 成功删除文件: %s\n原因: %s\n备份位置: %s.bak",
                filePath, reason, filePath);
            
        } catch (IOException e) {
            log.error("删除文件失败: {}", filePath, e);
            return "❌ 删除文件失败: " + e.getMessage();
        }
    }
    
    @Tool("列出目录下的所有文件。参数：directoryPath-目录路径")
    public String listDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            
            if (!Files.exists(path)) {
                return "❌ 错误：目录不存在 - " + directoryPath;
            }
            
            if (!Files.isDirectory(path)) {
                return "❌ 错误：路径不是目录 - " + directoryPath;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("📁 目录内容: ").append(directoryPath).append("\n\n");
            
            try (var stream = Files.list(path)) {
                List<Path> files = stream.sorted().toList();
                
                for (Path file : files) {
                    String type = Files.isDirectory(file) ? "📁" : "📄";
                    result.append(String.format("%s %s\n", type, file.getFileName()));
                }
                
                result.append(String.format("\n总计: %d 个文件/目录", files.size()));
            }
            
            return result.toString();
            
        } catch (IOException e) {
            log.error("列出目录失败: {}", directoryPath, e);
            return "❌ 列出目录失败: " + e.getMessage();
        }
    }
    
    /**
     * 安全检查：防止路径穿越攻击
     */
    private boolean isPathSafe(Path path) {
        try {
            Path normalizedPath = path.normalize().toAbsolutePath();
            String pathStr = normalizedPath.toString();
            
            // 禁止访问系统目录
            List<String> forbiddenPaths = List.of(
                "C:\\Windows",
                "C:\\Program Files",
                "/etc",
                "/usr",
                "/var"
            );
            
            for (String forbidden : forbiddenPaths) {
                if (pathStr.startsWith(forbidden)) {
                    log.warn("尝试访问禁止路径: {}", pathStr);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("路径安全检查失败", e);
            return false;
        }
    }
    
    /**
     * 备份文件
     */
    private void backupFile(Path path) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupPath = Paths.get(path.toString() + ".bak_" + timestamp);
        
        Files.copy(path, backupPath);
        log.info("备份文件: {} -> {}", path, backupPath);
    }
    
    /**
     * 记录操作日志
     */
    private void logOperation(String filePath, String description, String operation) {
        log.info("[代码写入工具] {} - {} - {}", operation, filePath, description);
        
        // 可以写入操作日志文件
        try {
            Path logFile = Paths.get("code-operations.log");
            String logEntry = String.format("[%s] %s: %s - %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                operation, filePath, description);
            
            Files.writeString(logFile, logEntry, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("写入操作日志失败", e);
        }
    }
}
