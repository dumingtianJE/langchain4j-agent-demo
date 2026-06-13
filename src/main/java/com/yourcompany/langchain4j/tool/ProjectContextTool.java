package com.yourcompany.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目上下文感知工具
 * 帮助 AI 理解项目结构、依赖和编码规范
 * 
 * 优化：工具返回值有长度限制，防止大段内容占用过多 Token
 */
@Slf4j
@Component
public class ProjectContextTool {

    /** 工具返回内容最大字符数 */
    private static final int MAX_OUTPUT_LENGTH = 4000;
    /** 分析编码规范时最多扫描的文件数 */
    private static final int MAX_CONVENTION_FILES = 10;
    /** 查找相似代码时最多扫描的文件数 */
    private static final int MAX_SEARCH_FILES = 30;
    /** 查找相似代码时最多返回的匹配数 */
    private static final int MAX_MATCH_RESULTS = 15;
    
    /**
     * 分析项目结构
     */
    @Tool("分析项目结构：识别项目类型、技术栈、构建工具等")
    public String analyzeProjectStructure(String projectPath) {
        try {
            log.info("开始分析项目结构，路径: {}", projectPath);
            
            Path path = Paths.get(projectPath);
            if (!Files.exists(path)) {
                log.warn("项目路径不存在: {}", projectPath);
                return "错误：项目路径不存在 - " + projectPath + 
                       "\n\n提示：请检查路径是否正确。在 Docker 环境中，请使用容器内的绝对路径，例如 /app/workspace/langchain4j-agent-demo 或 .（当前工作目录）";
            }
            
            Map<String, Object> projectInfo = new LinkedHashMap<>();
            
            // 检测构建工具
            if (Files.exists(path.resolve("pom.xml"))) {
                projectInfo.put("buildTool", "Maven");
                projectInfo.put("buildFile", "pom.xml");
            } else if (Files.exists(path.resolve("build.gradle")) || 
                       Files.exists(path.resolve("build.gradle.kts"))) {
                projectInfo.put("buildTool", "Gradle");
            } else if (Files.exists(path.resolve("package.json"))) {
                projectInfo.put("buildTool", "npm/yarn");
            }
            
            // 检测语言（单次目录遍历统计所有语言，避免 4 次 Files.walk）
            Map<String, Long> fileCounts = countFilesByExtension(path, Set.of(".java", ".py", ".js", ".ts"));
            long javaFiles = fileCounts.getOrDefault(".java", 0L);
            long pyFiles = fileCounts.getOrDefault(".py", 0L);
            long jsFiles = fileCounts.getOrDefault(".js", 0L);
            long tsFiles = fileCounts.getOrDefault(".ts", 0L);
            
            if (javaFiles > 0) projectInfo.put("language", "Java");
            else if (pyFiles > 0) projectInfo.put("language", "Python");
            else if (tsFiles > 0) projectInfo.put("language", "TypeScript");
            else if (jsFiles > 0) projectInfo.put("language", "JavaScript");
            
            // 检测框架
            if (Files.exists(path.resolve("src/main/java"))) {
                projectInfo.put("projectType", "Spring Boot / Java");
            }
            if (Files.exists(path.resolve("node_modules"))) {
                projectInfo.put("projectType", "Node.js");
            }
            
            // 检测配置文件
            List<String> configFiles = new ArrayList<>();
            if (Files.exists(path.resolve("application.yml"))) configFiles.add("application.yml");
            if (Files.exists(path.resolve("application.properties"))) configFiles.add("application.properties");
            if (Files.exists(path.resolve(".env"))) configFiles.add(".env");
            if (Files.exists(path.resolve("docker-compose.yml"))) configFiles.add("docker-compose.yml");
            projectInfo.put("configFiles", configFiles);
            
            // 统计代码行数
            projectInfo.put("javaFiles", javaFiles);
            projectInfo.put("pythonFiles", pyFiles);
            projectInfo.put("javascriptFiles", jsFiles);
            projectInfo.put("typescriptFiles", tsFiles);
            
            return formatProjectInfo(projectInfo);
            
        } catch (Exception e) {
            return "错误：分析项目结构失败 - " + e.getMessage();
        }
    }
    
    /**
     * 读取项目依赖
     */
    @Tool("读取项目依赖：从 pom.xml 或 package.json 提取依赖信息")
    public String readDependencies(String projectPath) {
        try {
            Path pomPath = Paths.get(projectPath, "pom.xml");
            if (Files.exists(pomPath)) {
                String pomContent = Files.readString(pomPath);
                
                // 提取关键依赖
                List<String> dependencies = new ArrayList<>();
                if (pomContent.contains("spring-boot-starter-web")) dependencies.add("Spring Web");
                if (pomContent.contains("spring-boot-starter-data-jpa")) dependencies.add("Spring Data JPA");
                if (pomContent.contains("spring-boot-starter-security")) dependencies.add("Spring Security");
                if (pomContent.contains("spring-boot-starter-data-redis")) dependencies.add("Spring Data Redis");
                if (pomContent.contains("langchain4j")) dependencies.add("LangChain4j");
                if (pomContent.contains("lombok")) dependencies.add("Lombok");
                if (pomContent.contains("mysql")) dependencies.add("MySQL");
                if (pomContent.contains("postgresql")) dependencies.add("PostgreSQL");
                
                return "项目依赖:\n" + dependencies.stream()
                    .map(d -> "  - " + d)
                    .collect(Collectors.joining("\n"));
            }
            
            return "未找到 pom.xml 文件";
            
        } catch (IOException e) {
            return "错误：读取依赖失败 - " + e.getMessage();
        }
    }
    
    /**
     * 获取项目编码规范
     */
    @Tool("获取项目编码规范：从项目文件推断命名规范、代码风格等")
    public String getCodingConventions(String projectPath) {
        try {
            Path srcPath = Paths.get(projectPath, "src/main/java");
            if (!Files.exists(srcPath)) {
                return "未找到 Java 源代码目录";
            }
            
            // 分析现有代码的命名规范
            List<String> classNames = new ArrayList<>();
            List<String> methodNames = new ArrayList<>();
            
            try (var paths = Files.walk(srcPath)) {
                paths.filter(p -> p.toString().endsWith(".java"))
                    .limit(MAX_CONVENTION_FILES) // 只分析有限文件数
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            // 提取类名
                            content.lines()
                                .filter(line -> line.contains("class ") || line.contains("interface "))
                                .forEach(line -> {
                                    String[] parts = line.split("\\s+");
                                    for (int i = 0; i < parts.length; i++) {
                                        if (parts[i].equals("class") || parts[i].equals("interface")) {
                                            if (i + 1 < parts.length) {
                                                classNames.add(parts[i + 1]);
                                            }
                                        }
                                    }
                                });
                        } catch (IOException e) {
                            // 忽略
                        }
                    });
            }
            
            StringBuilder conventions = new StringBuilder("【项目编码规范】\n\n");
            
            if (!classNames.isEmpty()) {
                conventions.append("类命名示例:\n");
                classNames.stream().limit(5).forEach(c -> conventions.append("  - ").append(c).append("\n"));
            }
            
            conventions.append("\n建议遵循的规范:\n");
            conventions.append("  - 类名：大驼峰（PascalCase）\n");
            conventions.append("  - 方法名：小驼峰（camelCase）\n");
            conventions.append("  - 常量：全大写下划线分隔（UPPER_SNAKE_CASE）\n");
            conventions.append("  - 包名：全小写，使用点分隔\n");
            conventions.append("  - 使用 JavaDoc 注释所有公开 API\n");
            conventions.append("  - 每行代码不超过 120 字符\n");
            
            return conventions.toString();
            
        } catch (Exception e) {
            return "错误：获取编码规范失败 - " + e.getMessage();
        }
    }
    
    /**
     * 查找相似代码示例
     */
    @Tool("在项目中查找相似的代码实现作为参考（限制扫描范围和返回数量）")
    public String findSimilarCodeExamples(String projectPath, String keyword) {
        try {
            Path srcPath = Paths.get(projectPath, "src/main/java");
            if (!Files.exists(srcPath)) {
                return "未找到源代码目录";
            }
            
            List<String> matches = new ArrayList<>();
            
            try (var paths = Files.walk(srcPath)) {
                paths.filter(p -> p.toString().endsWith(".java"))
                    .limit(MAX_SEARCH_FILES) // 限制扫描文件数
                    .forEach(p -> {
                        if (matches.size() >= MAX_MATCH_RESULTS) return; // 已够则跳过
                        try {
                            String content = Files.readString(p);
                            if (content.toLowerCase().contains(keyword.toLowerCase())) {
                                String relativePath = srcPath.relativize(p).toString();
                                matches.add("文件: " + relativePath);
                                content.lines()
                                    .filter(line -> line.contains(keyword))
                                    .limit(2)
                                    .forEach(line -> matches.add("  匹配: " + line.trim()));
                                matches.add("");
                            }
                        } catch (IOException e) {
                            // 忽略
                        }
                    });
            }
            
            if (matches.isEmpty()) {
                return "未找到包含 '" + keyword + "' 的代码示例";
            }
            
            return truncate("找到以下相关代码示例:\n\n" + 
                String.join("\n", matches), MAX_OUTPUT_LENGTH);
            
        } catch (Exception e) {
            return "错误：查找代码示例失败 - " + e.getMessage();
        }
    }
    
    /**
     * 单次目录遍历统计多种扩展名的文件数量（比多次 countFiles 调用高效得多）
     */
    private Map<String, Long> countFilesByExtension(Path directory, Set<String> extensions) throws IOException {
        if (!Files.exists(directory)) return Collections.emptyMap();
        
        Map<String, Long> counts = new HashMap<>();
        try (var paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                .forEach(p -> {
                    String fileName = p.getFileName().toString();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            counts.merge(ext, 1L, Long::sum);
                            break;
                        }
                    }
                });
        }
        return counts;
    }
    
    /**
     * 格式化项目信息
     */
    private String formatProjectInfo(Map<String, Object> info) {
        StringBuilder sb = new StringBuilder("【项目结构分析】\n\n");
        info.forEach((key, value) -> {
            sb.append(key).append(": ");
            if (value instanceof List) {
                sb.append("\n");
                ((List<?>) value).forEach(item -> sb.append("  - ").append(item).append("\n"));
            } else {
                sb.append(value).append("\n");
            }
        });
        return truncate(sb.toString(), MAX_OUTPUT_LENGTH);
    }

    /**
     * 截断字符串到最大长度
     */
    private String truncate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) return content;
        int cutPoint = content.lastIndexOf('\n', maxLength);
        if (cutPoint < maxLength / 2) cutPoint = maxLength;
        return content.substring(0, cutPoint) + "\n\n⚠️ 内容已截断（原始长度 " + content.length() + " 字符）。";
    }
}
