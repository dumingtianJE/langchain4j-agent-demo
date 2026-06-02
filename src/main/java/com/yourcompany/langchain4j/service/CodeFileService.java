package com.yourcompany.langchain4j.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * 代码文件服务：项目目录树、读写文件（路径限制在工作区根目录内）
 */
@Slf4j
@Service
public class CodeFileService {

    private static final Set<String> IGNORED_DIRS = Set.of(
            ".git", ".svn", ".idea", "node_modules", "target", "dist", "build",
            ".gradle", "__pycache__", ".venv", "venv", "coverage", ".mvn"
    );

    private static final Set<String> IGNORED_FILES = Set.of(
            ".DS_Store", "Thumbs.db"
    );

    private static final int MAX_TREE_DEPTH = 12;
    private static final long MAX_READ_BYTES = 2 * 1024 * 1024;

    private final Path workspaceRoot;

    public CodeFileService(@Value("${app.code.workspace-root:}") String workspaceRootConfig) {
        if (workspaceRootConfig != null && !workspaceRootConfig.isBlank()) {
            this.workspaceRoot = Paths.get(workspaceRootConfig).toAbsolutePath().normalize();
        } else {
            this.workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
        log.info("代码工作区根目录: {}", this.workspaceRoot);
    }

    public String getWorkspaceRoot() {
        return workspaceRoot.toString();
    }

    /**
     * 解析并校验路径，必须位于工作区根目录下
     */
    public Path resolveSafe(String inputPath) throws IOException {
        Path resolved;
        if (inputPath == null || inputPath.isBlank() || ".".equals(inputPath.trim())) {
            resolved = workspaceRoot;
        } else {
            Path candidate = Paths.get(inputPath.trim());
            resolved = candidate.isAbsolute()
                    ? candidate.normalize()
                    : workspaceRoot.resolve(candidate).normalize();
        }
        if (!resolved.startsWith(workspaceRoot)) {
            throw new IOException("路径越界，仅允许访问工作区: " + workspaceRoot);
        }
        if (!Files.exists(resolved)) {
            throw new IOException("路径不存在: " + resolved);
        }
        return resolved;
    }

    public Map<String, Object> buildDirectoryTree(String inputPath) throws IOException {
        Path root = resolveSafe(inputPath);
        if (!Files.isDirectory(root)) {
            throw new IOException("不是目录: " + root);
        }
        String relativeRoot = workspaceRoot.relativize(root).toString().replace('\\', '/');
        if (relativeRoot.isEmpty()) {
            relativeRoot = ".";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("root", relativeRoot);
        result.put("absolutePath", root.toString());
        result.put("workspaceRoot", workspaceRoot.toString());
        result.put("tree", buildTreeNode(root, 0));
        return result;
    }

    private Map<String, Object> buildTreeNode(Path dir, int depth) throws IOException {
        Map<String, Object> node = new LinkedHashMap<>();
        String rel = workspaceRoot.relativize(dir).toString().replace('\\', '/');
        if (rel.isEmpty()) {
            rel = workspaceRoot.getFileName().toString();
        }
        node.put("name", dir.getFileName() != null ? dir.getFileName().toString() : rel);
        node.put("path", rel.isEmpty() ? "." : rel);
        node.put("type", "directory");

        if (depth >= MAX_TREE_DEPTH) {
            node.put("children", List.of());
            return node;
        }

        List<Map<String, Object>> children = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> entries = stream
                    .filter(p -> !shouldIgnore(p))
                    .sorted((a, b) -> {
                        boolean aDir = Files.isDirectory(a);
                        boolean bDir = Files.isDirectory(b);
                        if (aDir != bDir) {
                            return aDir ? -1 : 1;
                        }
                        return a.getFileName().toString()
                                .compareToIgnoreCase(b.getFileName().toString());
                    })
                    .toList();

            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    children.add(buildTreeNode(entry, depth + 1));
                } else {
                    children.add(buildFileNode(entry));
                }
            }
        }
        node.put("children", children);
        return node;
    }

    private Map<String, Object> buildFileNode(Path file) {
        Map<String, Object> node = new LinkedHashMap<>();
        String rel = workspaceRoot.relativize(file).toString().replace('\\', '/');
        node.put("name", file.getFileName().toString());
        node.put("path", rel);
        node.put("type", "file");
        node.put("children", null);
        try {
            node.put("size", Files.size(file));
        } catch (IOException e) {
            node.put("size", 0);
        }
        return node;
    }

    private boolean shouldIgnore(Path path) {
        String name = path.getFileName().toString();
        if (IGNORED_FILES.contains(name)) {
            return true;
        }
        if (Files.isDirectory(path) && IGNORED_DIRS.contains(name)) {
            return true;
        }
        return name.startsWith(".") && !name.equals(".env.example");
    }

    public Map<String, Object> readFile(String inputPath) throws IOException {
        Path file = resolveSafe(inputPath);
        if (!Files.isRegularFile(file)) {
            throw new IOException("不是文件: " + file);
        }
        long size = Files.size(file);
        if (size > MAX_READ_BYTES) {
            throw new IOException("文件过大（>" + MAX_READ_BYTES + " 字节）: " + file);
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        String rel = workspaceRoot.relativize(file).toString().replace('\\', '/');
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", rel);
        result.put("content", content);
        result.put("size", size);
        return result;
    }

    public Map<String, Object> writeFile(String inputPath, String content) throws IOException {
        Path file = resolveSafe(inputPath);
        if (Files.exists(file) && Files.isDirectory(file)) {
            throw new IOException("目标是目录，无法写入: " + file);
        }
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, content != null ? content : "", StandardCharsets.UTF_8);
        String rel = workspaceRoot.relativize(file).toString().replace('\\', '/');
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", rel);
        result.put("message", "文件已保存");
        return result;
    }
}
