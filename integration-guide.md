# 智能体对接指南

本文档指导你编写的 Java 智能体如何对接本项目的 MCP Server 模块。

---

## 1. 项目概览

| 模块 | JAR 路径 | 功能 |
|------|---------|------|
| team-memory-server | `team-memory-server/target/team-memory-server-0.1.0.jar` | 团队记忆与架构决策的 CRUD |
| code-context-server | `code-context-server/target/code-context-server-0.1.0.jar` | 代码库索引与语义检索 |
| biz-integration-server | `biz-integration-server/target/biz-integration-server-0.1.0.jar` | 第三方业务系统集成（待实现） |

**运行环境**：JDK 17+，Maven 3.8+

**构建命令**：
```bash
mvn clean package -DskipTests
```

---

## 2. 对接方式

提供两种对接方式，根据你的智能体需求选择：

| | 方式 A：MCP Client | 方式 B：直接依赖 |
|---|---|---|
| 适用场景 | 智能体动态调用多个工具 | 只需固定调用某几个方法 |
| 工具发现 | 自动发现所有已注册 Tool | 无，需硬编码 |
| 通信方式 | stdio（子进程） | JVM 内部调用 |
| 引入依赖 | MCP SDK（client 侧） | 直接依赖子模块 |

---

## 3. 方式 A：作为 MCP Client 调用

### 3.1 添加依赖

在你的智能体项目 `pom.xml` 中：

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>0.10.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.0</version>
</dependency>
```

### 3.2 核心代码

```java
package your.agent;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.*;

import java.util.Map;

public class McpToolCaller {

    /**
     * 启动一个 MCP Server 并创建连接。
     *
     * @param jarPath MCP Server 的 JAR 绝对路径
     * @return MCP 同步客户端
     */
    public static McpSyncClient connect(String jarPath) {
        // 启动 MCP Server 作为子进程
        Process process = new ProcessBuilder("java", "-jar", jarPath)
                .redirectErrorStream(true) // stderr 合并到 stdout（调试用）
                .start();

        // 创建 stdio 传输层
        StdioClientTransport transport = new StdioClientTransport(process);

        // 创建同步客户端
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();

        return client;
    }

    /**
     * 列出 Server 提供的所有工具。
     */
    public static ListToolsResult listTools(McpSyncClient client) {
        return client.listTools();
    }

    /**
     * 调用指定工具。
     *
     * @param client    MCP 客户端
     * @param toolName  工具名称，如 "add_team_memory"
     * @param arguments 工具参数 Map
     * @return 调用结果
     */
    public static CallToolResult callTool(McpSyncClient client,
                                          String toolName,
                                          Map<String, Object> arguments) {
        return client.callTool(toolName, arguments);
    }

    /**
     * 关闭连接。
     */
    public static void close(McpSyncClient client) {
        client.close();
    }
}
```

### 3.3 使用示例

```java
// ──── 1. 连接 team-memory-server ────
McpSyncClient memoryClient = McpToolCaller.connect(
    "D:/workspace/claude-code-stack-zh-main/team-memory-server/target/team-memory-server-0.1.0.jar"
);

// 查看可用工具
ListToolsResult tools = McpToolCaller.listTools(memoryClient);
tools.tools().forEach(t -> System.out.println(t.name() + ": " + t.description()));

// 新增团队记忆
McpToolCaller.callTool(memoryClient, "add_team_memory", Map.of(
    "project_name", "我的项目",
    "decision_content", "使用 Redis 做分布式缓存，MySQL 做持久化",
    "tags", "架构,缓存,Redis"
));

// 查询项目记忆
CallToolResult result = McpToolCaller.callTool(memoryClient, "get_project_memories", Map.of(
    "project_name", "我的项目"
));
result.content().forEach(c -> System.out.println(((TextContent) c).text()));

// 按标签搜索
McpToolCaller.callTool(memoryClient, "search_memories_by_tag", Map.of("tag", "Redis"));

// 删除记忆
McpToolCaller.callTool(memoryClient, "delete_team_memory", Map.of("memory_id", 1));

// ──── 2. 连接 code-context-server ────
McpSyncClient contextClient = McpToolCaller.connect(
    "D:/workspace/claude-code-stack-zh-main/code-context-server/target/code-context-server-0.1.0.jar"
);

// 索引代码库
McpToolCaller.callTool(contextClient, "index_codebase", Map.of(
    "dir_path", "D:/your-project/src"
));

// 语义搜索
CallToolResult searchResult = McpToolCaller.callTool(contextClient, "search_code_context", Map.of(
    "query", "用户登录认证逻辑",
    "top_k", 5
));
searchResult.content().forEach(c -> System.out.println(((TextContent) c).text()));

// ──── 3. 连接 biz-integration-server ────
McpSyncClient bizClient = McpToolCaller.connect(
    "D:/workspace/claude-code-stack-zh-main/biz-integration-server/target/biz-integration-server-0.1.0.jar"
);

// 查询业务数据
McpToolCaller.callTool(bizClient, "biz_query", Map.of("id", "ORDER-12345"));

// 创建业务数据
McpToolCaller.callTool(bizClient, "biz_create", Map.of(
    "name", "新订单",
    "content", "客户张三订购产品A，数量10"
));

// ──── 关闭 ────
McpToolCaller.close(memoryClient);
McpToolCaller.close(contextClient);
McpToolCaller.close(bizClient);
```

### 3.4 智能体集成模式

推荐在你的智能体中实现 **Tool Registry** 模式：

```java
public class AgentToolRegistry {
    private final Map<String, McpSyncClient> clients = new HashMap<>();
    private final Map<String, String> toolToClientMap = new HashMap<>();

    /**
     * 注册一个 MCP Server，自动发现其所有工具。
     */
    public void registerServer(String name, String jarPath) {
        McpSyncClient client = McpToolCaller.connect(jarPath);
        clients.put(name, client);

        // 自动注册该 Server 的所有工具
        client.listTools().tools().forEach(tool -> {
            toolToClientMap.put(tool.name(), name);
        });
    }

    /**
     * 智能体根据 AI 决策调用工具。
     */
    public String execute(String toolName, Map<String, Object> args) {
        String serverName = toolToClientMap.get(toolName);
        if (serverName == null) {
            return "未知工具: " + toolName;
        }

        McpSyncClient client = clients.get(serverName);
        CallToolResult result = client.callTool(toolName, args);

        // 提取文本结果
        return result.content().stream()
            .filter(c -> c instanceof TextContent)
            .map(c -> ((TextContent) c).text())
            .collect(Collectors.joining("\n"));
    }

    /**
     * 获取所有可用工具的描述（可注入到 LLM 的 system prompt 中）。
     */
    public String getToolDescriptions() {
        StringBuilder sb = new StringBuilder("可用工具列表：\n");
        for (var entry : clients.entrySet()) {
            McpSyncClient client = entry.getValue();
            client.listTools().tools().forEach(tool -> {
                sb.append(String.format("- %s: %s\n", tool.name(), tool.description()));
            });
        }
        return sb.toString();
    }

    public void closeAll() {
        clients.values().forEach(McpSyncClient::close);
    }
}
```

使用：

```java
AgentToolRegistry registry = new AgentToolRegistry();
registry.registerServer("memory", "D:/.../team-memory-server-0.1.0.jar");
registry.registerServer("context", "D:/.../code-context-server-0.1.0.jar");
registry.registerServer("biz", "D:/.../biz-integration-server-0.1.0.jar");

// 将工具描述注入 LLM prompt
String systemPrompt = "你是一个智能助手...\n\n" + registry.getToolDescriptions();

// AI 返回需要调用的工具
String result = registry.execute("add_team_memory", Map.of(
    "project_name", "项目A",
    "decision_content", "使用微服务架构"
));
```

---

## 4. 方式 B：直接依赖（Maven 模块引用）

### 4.1 添加依赖

在你的智能体项目 `pom.xml` 中直接引入子模块：

```xml
<!-- 团队记忆 -->
<dependency>
    <groupId>com.claude.code.stack.zh</groupId>
    <artifactId>team-memory-server</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- 代码上下文（可选） -->
<dependency>
    <groupId>com.claude.code.stack.zh</groupId>
    <artifactId>code-context-server</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- 业务集成（可选） -->
<dependency>
    <groupId>com.claude.code.stack.zh</groupId>
    <artifactId>biz-integration-server</artifactId>
    <version>0.1.0</version>
</dependency>
```

> **前提**：先在本项目根目录执行 `mvn install` 将模块安装到本地 Maven 仓库。

### 4.2 使用示例

```java
// ──── 团队记忆直接调用 ────
import com.claude.stack.zh.memory.MemoryRepository;
import com.claude.stack.zh.memory.Memory;

MemoryRepository repo = new MemoryRepository();

// 新增
int id = repo.addMemory("我的项目", "使用 Redis 做缓存", "架构,缓存");

// 查询
List<Memory> memories = repo.getMemoriesByProject("我的项目");
memories.forEach(m -> System.out.println(m.toDisplayString()));

// 按标签搜索
List<Memory> found = repo.searchMemoriesByTag("Redis");

// 删除
repo.deleteMemory(id);

// ──── 代码上下文直接调用 ────
import com.claude.stack.zh.context.CodeIndexer;
import com.claude.stack.zh.context.VectorStore;
import com.claude.stack.zh.context.CodeChunk;

CodeIndexer indexer = new CodeIndexer();
VectorStore store = new VectorStore();

// 索引
List<CodeChunk> chunks = indexer.indexDirectory("D:/your-project/src");
store.addChunks(chunks);

// 搜索
List<CodeChunk> results = store.search("用户认证逻辑", 5);
results.forEach(c -> System.out.println(c.getFilePath() + ":" + c.getStartLine()));

// ──── 业务集成直接调用 ────
import com.claude.stack.zh.biz.BizClient;

BizClient client = new BizClient("https://api.example.com/v1", "your-token");
String data = client.queryById("12345");
String created = client.create("标题", "内容");
```

---

## 5. 可用工具清单

### 5.1 team-memory-server

| 工具名 | 参数 | 说明 |
|--------|------|------|
| `add_team_memory` | `project_name`(string, 必填), `decision_content`(string, 必填), `tags`(string, 可选) | 新增团队记忆 |
| `get_project_memories` | `project_name`(string, 必填) | 查询项目全部记忆 |
| `search_memories_by_tag` | `tag`(string, 必填) | 按标签搜索记忆 |
| `delete_team_memory` | `memory_id`(integer, 必填) | 删除指定记忆 |

### 5.2 code-context-server

| 工具名 | 参数 | 说明 |
|--------|------|------|
| `index_codebase` | `dir_path`(string, 必填) | 索引代码库（耗时操作） |
| `search_code_context` | `query`(string, 必填), `top_k`(integer, 可选, 默认5) | 语义检索代码片段 |

### 5.3 biz-integration-server（示例，待实现）

| 工具名 | 参数 | 说明 |
|--------|------|------|
| `biz_query` | `id`(string, 必填) | 根据 ID 查询业务数据 |
| `biz_create` | `name`(string, 必填), `content`(string, 必填) | 创建业务数据 |

---

## 6. 环境变量配置

| 变量名 | 用途 | 所属模块 |
|--------|------|---------|
| `BIZ_API_BASE_URL` | 第三方系统 API 地址 | biz-integration-server |
| `BIZ_API_TOKEN` | 认证令牌（Bearer Token） | biz-integration-server |

PowerShell 设置示例：
```powershell
$env:BIZ_API_BASE_URL = "https://api.example.com/v1"
$env:BIZ_API_TOKEN = "your-token-here"
```

---

## 7. 常见问题

### Q: 方式 A 子进程启动后如何保证通信正常？
A: MCP 使用 **stdio** 协议，Server 进程的 stdin 接收请求、stdout 发送响应。确保你的智能体不会读取 Server 进程的 stdout 以外的内容。Server 的日志默认输出到 stderr，不会干扰通信。

### Q: 多个 MCP Server 可以同时运行吗？
A: 可以。每个 Server 是独立的子进程，互不干扰。你可以在智能体中同时持有多个 `McpSyncClient` 实例。

### Q: 方式 B 直接依赖时数据存在哪里？
A: `team-memory-server` 使用 SQLite 数据库，默认存储在当前工作目录下的 `team_memories.db` 文件。`code-context-server` 的索引数据存储在 `./code_index_data/` 目录。

### Q: 如何新增自定义工具？
A: 在对应的 MCP Server 类中：
1. 在 `BizClient.java` 添加业务方法
2. 在 `BizMcpServer.java` 中参照现有 Tool 方法新增一个 `SyncToolSpecification`
3. 在 `main()` 方法的 `.tools(...)` 中注册

重新 `mvn package` 即可生效。
