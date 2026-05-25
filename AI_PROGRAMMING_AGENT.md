# AI 编程 Agent 系统

一个功能完整的 AI 编程助手系统，基于 Spring Boot + LangChain4j 构建，集成 LLM、MCP 工具、Skill 管理、知识库 RAG、自主学习和 AI 监管能力。

## 🎯 核心功能

### 1. **LLM 集成**
- 支持通义千问 qwen-max 模型
- OpenAI 兼容接口
- 可自定义模型参数（temperature、timeout 等）

### 2. **MCP 工具集成**
- **CodeFileTool**: 文件读写、目录浏览
- **CodeAnalysisTool**: 代码统计、复杂度分析、TODO 检测
- **KnowledgeBaseTool**: 知识库检索工具

### 3. **Skill 技能管理系统**
- 预置多种开发技能（Spring Boot、FastAPI、React、设计模式、SQL 优化等）
- 技能注册、检索、分类管理
- 使用统计和熟练度跟踪

### 4. **知识库系统（RAG）- 核心功能**
- 向量相似度检索
- 文档分块和 Embedding
- 支持多种文档分类（技术文档、最佳实践、项目规范等）
- 自动加载默认知识（LangChain4j 指南、Spring Boot 最佳实践）
- 支持动态添加和删除文档

### 5. **自主学习能力**
- 记录用户交互经验
- 基于反馈自动学习（高分反馈转化为知识）
- 负面反馈分析和改进模式提取
- 学习报告生成
- 手动学习新知识

### 6. **AI 监管系统**
- Token 使用监控和统计
- 多层次限制（单次请求、每小时、每日）
- 并发请求控制
- 自动警报机制
- 使用报告生成

## 📁 项目结构

```
src/main/java/com/yourcompany/langchain4j/
├── agent/
│   └── AiProgrammingAgent.java          # AI 编程 Agent 接口
├── config/
│   └── AiProgrammingAgentConfig.java    # Agent 配置类
├── controller/
│   └── AiProgrammingAgentController.java # REST API 控制器
├── tool/
│   ├── CodeFileTool.java                # 文件操作工具
│   ├── CodeAnalysisTool.java            # 代码分析工具
│   ├── KnowledgeBaseTool.java           # 知识库检索工具
│   └── OrderTool.java                   # 订单工具（原有）
├── skill/
│   ├── Skill.java                       # 技能实体
│   └── SkillManager.java                # 技能管理器
├── knowledge/
│   ├── KnowledgeDocument.java           # 知识文档实体
│   └── KnowledgeBaseManager.java        # 知识库管理器（RAG 核心）
├── learning/
│   ├── LearningExperience.java          # 学习经验实体
│   └── SelfLearningManager.java         # 自主学习管理器
├── supervisor/
│   ├── TokenUsageRecord.java            # Token 使用记录
│   └── AiSupervisor.java                # AI 监管器
└── ...
```

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- 通义千问 API 密钥

### 配置

在 `application.yml` 中配置 API 密钥：

```yaml
langchain4j.open-ai.chat-model.api-key=your-api-key
```

或设置环境变量：

```bash
export DASHSCOPE_API_KEY=your-api-key
```

### 启动项目

```bash
mvn spring-boot:run
```

## 📡 API 接口

### 编程任务

**POST** `/api/ai-programming-agent/execute`

```json
{
  "userId": "user123",
  "task": "创建一个 Spring Boot REST Controller，包含 CRUD 操作",
  "context": "使用 Spring Boot 3.x 和 Java 17"
}
```

### 代码审查

**POST** `/api/ai-programming-agent/review-code`

```json
{
  "code": "public class UserService { ... }",
  "language": "java"
}
```

### 技术问题解答

**POST** `/api/ai-programming-agent/answer-question`

```json
{
  "question": "如何在 Spring Boot 中实现 Redis 缓存？"
}
```

### 知识库检索

**GET** `/api/ai-programming-agent/knowledge/search?query=Spring Boot 最佳实践`

### 添加知识文档

**POST** `/api/ai-programming-agent/knowledge/add`

```json
{
  "title": "Microservices Architecture Guide",
  "content": "...",
  "category": "架构设计",
  "tags": ["microservices", "architecture"]
}
```

### 技能管理

**GET** `/api/ai-programming-agent/skills/search?query=java`

**POST** `/api/ai-programming-agent/skills/add`

```json
{
  "name": "GraphQL API 开发",
  "description": "使用 GraphQL 构建灵活的 API",
  "category": "框架",
  "content": "...",
  "keywords": ["graphql", "api", "backend"],
  "proficiencyLevel": 4
}
```

### 学习反馈

**POST** `/api/ai-programming-agent/learning/feedback`

```json
{
  "query": "如何优化数据库查询？",
  "response": "...",
  "userFeedback": "回答非常详细，提供了多种优化方案",
  "feedbackScore": 5,
  "category": "数据库优化"
}
```

### 监管统计

**GET** `/api/ai-programming-agent/supervisor/stats`

**GET** `/api/ai-programming-agent/supervisor/report`

## 🔧 AI 监管配置

在 `application.yml` 中配置限制：

```yaml
ai.supervisor.max-tokens-per-request=10000    # 单次请求上限
ai.supervisor.max-tokens-per-hour=100000      # 每小时上限
ai.supervisor.max-tokens-per-day=1000000      # 每日上限
ai.supervisor.max-concurrent-requests=10      # 最大并发请求
ai.supervisor.alert-threshold=0.8             # 警报阈值（80%）
```

## 📊 系统特性

### 知识库 RAG 流程
1. 用户提问
2. 生成问题的向量 Embedding
3. 在向量数据库中检索相似文档
4. 将相关文档作为上下文提供给 LLM
5. LLM 生成基于知识的回答

### 自主学习流程
1. 记录用户交互（查询、响应、使用的工具和技能）
2. 收集用户反馈（1-5 分）
3. 高分反馈（≥4）自动转化为知识文档
4. 低分反馈（≤2）分析改进模式
5. 生成学习报告

### AI 监管流程
1. 请求前检查限制（Token、并发）
2. 执行过程中监控 Token 使用
3. 超过阈值触发警报
4. 记录使用统计
5. 生成监管报告

## 🎓 使用示例

### 示例 1：代码生成

```bash
curl -X POST http://localhost:8080/api/ai-programming-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "dev001",
    "task": "创建一个用户注册接口，包含密码加密和邮箱验证",
    "context": "Spring Boot + Spring Security + JPA"
  }'
```

### 示例 2：知识库检索

```bash
curl "http://localhost:8080/api/ai-programming-agent/knowledge/search?query=如何配置多数据源"
```

### 示例 3：代码审查

```bash
curl -X POST http://localhost:8080/api/ai-programming-agent/review-code \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class UserController { @Autowired private UserService userService; }",
    "language": "java"
  }'
```

### 示例 4：查看监管报告

```bash
curl http://localhost:8080/api/ai-programming-agent/supervisor/report
```

## 🏗️ 扩展建议

### 生产环境优化

1. **持久化向量存储**
   - 替换 InMemoryEmbeddingStore 为 Milvus、Pinecone 或 Qdrant
   - 实现文档持久化

2. **数据库集成**
   - 将 Skill、KnowledgeDocument、LearningExperience 存储到数据库
   - 实现数据备份和恢复

3. **缓存优化**
   - 使用 Redis 缓存频繁检索的知识文档
   - 缓存 Embedding 向量

4. **监控增强**
   - 集成 Prometheus + Grafana
   - 实时监控 Token 使用和系统性能

5. **安全加固**
   - 添加 API 认证和授权
   - 实现请求频率限制
   - 敏感信息加密

## 📝 开发计划

- [ ] 支持更多 LLM 模型（GPT-4、Claude 等）
- [ ] MCP 协议完整集成
- [ ] 代码执行沙箱
- [ ] Git 集成（代码提交、PR 生成）
- [ ] 多 Agent 协作
- [ ] 可视化 Dashboard

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
