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
 */
@Component
public class CodeFileTool {

    @Tool("读取指定路径的代码文件内容")
    public String readFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + filePath;
            }
            return Files.readString(path);
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
                return paths
                    .map(p -> (Files.isDirectory(p) ? "[DIR] " : "[FILE] ") + p.getFileName())
                    .collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            return "错误：列出文件失败 - " + e.getMessage();
        }
    }

    @Tool("递归列出目录下的所有文件，支持过滤器")
    public String listFilesRecursively(String directoryPath, String extension) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return "错误：目录不存在 - " + directoryPath;
            }
            
            try (Stream<Path> paths = Files.walk(path)) {
                List<String> fileList = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(f -> extension == null || f.endsWith(extension))
                    .collect(Collectors.toList());
                
                return String.join("\n", fileList);
            }
        } catch (IOException e) {
            return "错误：递归列出文件失败 - " + e.getMessage();
        }
    }

    @Tool("检查文件或目录是否存在")
    public boolean fileExists(String path) {
        return Files.exists(Paths.get(path));
    }
}
