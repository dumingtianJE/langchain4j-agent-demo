package com.yourcompany.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MCP 工具集成 - 代码文件操作工具
 * 提供文件读写、目录浏览等能力
 * 
 * 优化：所有返回值均有长度限制，防止大文件内容占用过多 Token
 */
@Component
public class CodeFileTool {

    /** 工具返回内容最大字符数，超过后自动截断并提示 */
    private static final int MAX_OUTPUT_LENGTH = 4000;
    /** 递归列出文件时的最大深度 */
    private static final int MAX_WALK_DEPTH = 5;
    /** 递归列出文件时的最大文件数 */
    private static final int MAX_FILE_COUNT = 100;

    @Tool("读取指定路径的代码文件内容（大文件自动截断到前200行）")
    public String readFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + filePath;
            }
            String content = Files.readString(path);
            // 大文件截断：保留前200行，避免占用过多 Token
            String[] lines = content.split("\n");
            if (lines.length > 200) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 200; i++) {
                    sb.append(lines[i]).append("\n");
                }
                return truncate(sb.toString(), MAX_OUTPUT_LENGTH)
                        + "\n\n⚠️ 文件共 " + lines.length + " 行，已截断前 200 行。如需查看特定行范围，请明确指定。";
            }
            return truncate(content, MAX_OUTPUT_LENGTH);
        } catch (IOException e) {
            return "错误：读取文件失败 - " + e.getMessage();
        }
    }

    @Tool("写入内容到指定路径的文件")
    public String writeFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return "成功：文件已写入 - " + filePath;
        } catch (IOException e) {
            return "错误：写入文件失败 - " + e.getMessage();
        }
    }

    @Tool("列出指定目录下的文件")
    public String listFiles(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return "错误：目录不存在 - " + directoryPath;
            }
            
            try (Stream<Path> paths = Files.list(path)) {
                String result = paths
                    .map(p -> (Files.isDirectory(p) ? "[DIR] " : "[FILE] ") + p.getFileName())
                    .collect(Collectors.joining("\n"));
                return truncate(result, MAX_OUTPUT_LENGTH);
            }
        } catch (IOException e) {
            return "错误：列出文件失败 - " + e.getMessage();
        }
    }

    @Tool("递归列出目录下的所有文件，支持按扩展名过滤（限制深度5层、最多100个文件）")
    public String listFilesRecursively(String directoryPath, String extension) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return "错误：目录不存在 - " + directoryPath;
            }
            
            try (Stream<Path> paths = Files.walk(path, MAX_WALK_DEPTH)) {
                List<String> fileList = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(f -> extension == null || f.endsWith(extension))
                    .limit(MAX_FILE_COUNT)
                    .collect(Collectors.toList());
                
                String result = String.join("\n", fileList);
                if (fileList.size() >= MAX_FILE_COUNT) {
                    result += "\n\n⚠️ 结果已截断，仅显示前 " + MAX_FILE_COUNT + " 个文件。";
                }
                return truncate(result, MAX_OUTPUT_LENGTH);
            }
        } catch (IOException e) {
            return "错误：递归列出文件失败 - " + e.getMessage();
        }
    }

    @Tool("检查文件或目录是否存在")
    public boolean fileExists(String path) {
        return Files.exists(Paths.get(path));
    }

    /**
     * 截断字符串到最大长度，保留完整性
     */
    private String truncate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) return content;
        // 在行边界截断
        int cutPoint = content.lastIndexOf('\n', maxLength);
        if (cutPoint < maxLength / 2) cutPoint = maxLength;
        return content.substring(0, cutPoint) + "\n\n⚠️ 内容已截断（原始长度 " + content.length() + " 字符）。如需完整内容请分段读取。";
    }
}
