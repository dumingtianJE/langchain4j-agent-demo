package com.yourcompany.langchain4j.tool;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 增强版代码文件写入工具
 *
 * 核心能力：
 * 1. 写入前编译检查 - 防止有语法错误的代码写入
 * 2. 写入后自动格式化 - Google Java Format
 * 3. Diff 预览 - 修改现有文件前记录差异
 * 4. 写入白名单 - 限制只能写入安全目录
 * 5. IDEA 文件变更通知 - 触发项目刷新
 */
@Slf4j
@Component
public class CodeWriteTool {

    private static final List<String> ALLOWED_DIRECTORIES = List.of(
            "src/main/java",
            "src/main/resources",
            "src/test/java",
            "src/test/resources"
    );

    @Tool("将AI生成的Java代码写入指定文件路径。支持创建新文件和覆盖现有文件。写入前自动编译检查语法，写入后自动格式化。参数：filePath-文件路径, codeContent-代码内容, description-描述")
    public String writeCodeToFile(String filePath, String codeContent, String description) {
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();

            // 1. 白名单检查
            String whitelistError = checkWhitelist(path);
            if (whitelistError != null) {
                return whitelistError;
            }

            // 2. 编译检查（仅 .java 文件）
            if (filePath.endsWith(".java")) {
                String compileError = compileCheck(codeContent, filePath);
                if (compileError != null) {
                    return compileError;
                }
            }

            // 3. 自动格式化（仅 .java 文件）
            if (filePath.endsWith(".java")) {
                codeContent = formatJavaCode(codeContent);
            }

            // 4. Diff 记录（修改现有文件时）
            String diffOutput = recordDiff(path, codeContent);

            // 5. 备份现有文件
            if (Files.exists(path)) {
                backupFile(path);
            }

            // 6. 创建目录
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 7. 写入文件
            Files.writeString(path, codeContent, StandardCharsets.UTF_8);

            // 8. 通知 IDEA 刷新
            notifyIdeaRefresh(path);

            // 9. 记录操作日志
            logOperation(filePath, description, "WRITE");

            // 10. 构建响应
            StringBuilder result = new StringBuilder();
            result.append(String.format("✅ 成功写入文件: %s\n", filePath));
            result.append(String.format("描述: %s\n", description));
            result.append(String.format("大小: %d 字节\n", codeContent.length()));

            if (diffOutput != null && !diffOutput.isEmpty()) {
                result.append("\n📝 变更差异:\n").append(diffOutput);
            }

            return result.toString();

        } catch (IOException e) {
            log.error("写入文件失败: {}", filePath, e);
            return "❌ 写入文件失败: " + e.getMessage();
        }
    }

    /** 读取文件时的最大返回字符数 */
    private static final int MAX_READ_OUTPUT = 5000;

    @Tool("读取指定文件的内容，用于查看现有代码或验证写入结果。参数：filePath-文件路径")
    public String readFileContent(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "❌ 错误：文件不存在 - " + filePath;
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            // 大文件截断
            if (content.length() > MAX_READ_OUTPUT) {
                content = content.substring(0, MAX_READ_OUTPUT)
                        + "\n\n⚠️ 文件内容已截断（总长 " + content.length() + " 字符）。如需查看特定部分请指定行范围。";
            }
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
            Path path = Paths.get(filePath).toAbsolutePath().normalize();

            String whitelistError = checkWhitelist(path);
            if (whitelistError != null) {
                return whitelistError;
            }

            if (!Files.exists(path)) {
                return "❌ 错误：文件不存在，请先创建文件";
            }

            Files.writeString(path, codeContent, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            notifyIdeaRefresh(path);
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
            Path path = Paths.get(filePath).toAbsolutePath().normalize();

            String whitelistError = checkWhitelist(path);
            if (whitelistError != null) {
                return whitelistError;
            }

            if (!Files.exists(path)) {
                return "❌ 错误：文件不存在 - " + filePath;
            }

            backupFile(path);
            Files.delete(path);
            notifyIdeaRefresh(path);
            logOperation(filePath, reason, "DELETE");

            return String.format("✅ 成功删除文件: %s\n原因: %s\n备份位置: %s.bak", filePath, reason, filePath);
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

    @Tool("验证Java代码是否有语法错误，不写入文件。参数：codeContent-Java代码内容, fileName-文件名")
    public String validateJavaCode(String codeContent, String fileName) {
        String error = compileCheck(codeContent, fileName);
        if (error != null) {
            return error;
        }
        return "✅ Java 代码语法检查通过，可以安全写入";
    }

    @Tool("预览两个Java代码版本的差异，不写入文件。参数：filePath-目标文件路径, newCode-新代码内容")
    public String previewDiff(String filePath, String newCode) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "ℹ️ 文件不存在，写入时将创建新文件（无差异可显示）";
            }
            String oldContent = Files.readString(path, StandardCharsets.UTF_8);
            List<String> oldLines = oldContent.lines().toList();
            List<String> newLines = newCode.lines().toList();

            Patch<String> patch = DiffUtils.diff(oldLines, newLines);
            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                    filePath + " (当前)", filePath + " (新版本)", oldLines, patch, 3);

            if (unifiedDiff.isEmpty()) {
                return "ℹ️ 代码内容完全相同，无需修改";
            }

            return String.join("\n", unifiedDiff);
        } catch (IOException e) {
            return "❌ 生成差异失败: " + e.getMessage();
        }
    }

    // ==================== 增强功能实现 ====================

    /**
     * 白名单检查：确保文件路径在允许的目录下
     */
    private String checkWhitelist(Path path) {
        String pathStr = path.toString().replace('\\', '/');

        for (String allowedDir : ALLOWED_DIRECTORIES) {
            if (pathStr.contains(allowedDir)) {
                return null;
            }
        }

        log.warn("写入路径不在白名单内: {}", pathStr);
        return String.format(
                "❌ 安全限制：不允许写入该路径\n路径: %s\n允许目录: %s\n请使用项目 src/ 目录下的路径",
                pathStr, String.join(", ", ALLOWED_DIRECTORIES));
    }

    /**
     * 编译检查：使用 Java Compiler API 验证代码语法
     * 返回 null 表示通过，否则返回错误信息
     */
    private String compileCheck(String codeContent, String fileName) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                log.warn("系统未提供 JavaCompiler，跳过编译检查");
                return null;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            JavaFileObject sourceFile = new SimpleJavaFileObject(
                    java.net.URI.create("string:///" + fileName), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return codeContent;
                }
            };

            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            StringWriter output = new StringWriter();

            JavaCompiler.CompilationTask task = compiler.getTask(
                    output, fileManager, diagnostics, null, null, List.of(sourceFile));

            boolean success = task.call();

            if (!success) {
                StringBuilder errors = new StringBuilder("❌ 编译检查失败，代码有语法错误：\n\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        errors.append(String.format("  行 %d: %s\n",
                                diagnostic.getLineNumber(),
                                diagnostic.getMessage(null)));
                    }
                }
                errors.append("\n请修复以上错误后再写入");
                return errors.toString();
            }

            log.info("编译检查通过: {}", fileName);
            return null;

        } catch (Exception e) {
            log.warn("编译检查异常，跳过: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 自动格式化：使用 Google Java Format
     */
    private String formatJavaCode(String codeContent) {
        try {
            Formatter formatter = new Formatter();
            String formatted = formatter.formatSource(codeContent);
            log.info("代码格式化完成");
            return formatted;
        } catch (FormatterException e) {
            log.warn("代码格式化失败，保留原始代码: {}", e.getMessage());
            return codeContent;
        }
    }

    /**
     * Diff 记录：生成统一差异格式
     * 返回 null 如果是新文件或无差异
     */
    private String recordDiff(Path path, String newContent) {
        try {
            if (!Files.exists(path)) {
                return null;
            }

            String oldContent = Files.readString(path, StandardCharsets.UTF_8);
            List<String> oldLines = oldContent.lines().toList();
            List<String> newLines = newContent.lines().toList();

            Patch<String> patch = DiffUtils.diff(oldLines, newLines);
            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                    path.getFileName() + " (修改前)",
                    path.getFileName() + " (修改后)",
                    oldLines, patch, 3);

            if (unifiedDiff.isEmpty()) {
                return null;
            }

            // 保存 diff 到日志文件
            String diffLog = String.join("\n", unifiedDiff);
            saveDiffLog(path, diffLog);

            return diffLog;

        } catch (IOException e) {
            log.warn("生成 Diff 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存 Diff 日志到独立文件
     */
    private void saveDiffLog(Path filePath, String diffContent) {
        try {
            Path diffDir = Paths.get("code-changes");
            Files.createDirectories(diffDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String diffFileName = filePath.getFileName().toString() + "_" + timestamp + ".diff";
            Path diffFile = diffDir.resolve(diffFileName);

            Files.writeString(diffFile, diffContent, StandardCharsets.UTF_8);
            log.info("Diff 日志已保存: {}", diffFile);
        } catch (IOException e) {
            log.warn("保存 Diff 日志失败: {}", e.getMessage());
        }
    }

    /**
     * IDEA 文件变更通知
     * 写入 .idea-refresh 标记文件，IDEA 的文件监视器会自动检测变更
     */
    private void notifyIdeaRefresh(Path filePath) {
        try {
            Path marker = Paths.get(".idea-refresh");
            String content = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    + " " + filePath.toString() + "\n";
            Files.writeString(marker, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            log.info("IDEA 刷新标记已更新: {}", filePath);
        } catch (IOException e) {
            log.warn("IDEA 通知失败: {}", e.getMessage());
        }
    }

    /**
     * 备份文件（带时间戳）
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
        try {
            Path logFile = Paths.get("code-operations.log");
            String logEntry = String.format("[%s] %s: %s - %s%n",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    operation, filePath, description);
            Files.writeString(logFile, logEntry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("写入操作日志失败", e);
        }
    }
}
