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

## 🏗️ 生产环境搭建指南

### ✅ 已实现的生产级功能

本项目已经实现了以下生产环境核心功能：

#### 1. **持久化向量存储**
- ✅ 基于文件系统的 Embedding 存储（`./data/embeddings`）
- ✅ 定时自动保存（每 5 分钟）
- ✅ 应用关闭时自动保存
- ✅ 启动时自动加载历史数据
- 📝 **生产建议**：替换为专业向量数据库（Milvus/Qdrant/Pinecone）

#### 2. **数据库集成**
- ✅ H2 文件数据库（`./data/ai-agent-db`）
- ✅ JPA 实体映射（Skill、KnowledgeDocument、LearningExperience）
- ✅ Repository 层完整实现
- ✅ H2 控制台（开发环境）：`http://localhost:8080/h2-console`
- 📝 **生产建议**：替换为 PostgreSQL/MySQL

#### 3. **Redis 缓存**
- ✅ 多级缓存策略（知识 2h、技能 4h、会话 30min、统计 5min）
- ✅ JSON 序列化支持
- ✅ 统一缓存操作服务（CacheService）
- 📝 **生产建议**：配置 Redis 集群和持久化

#### 4. **Prometheus 监控**
- ✅ 完整的业务指标收集（请求、Token、知识库、技能等）
- ✅ Counter、Timer、Gauge 多维度指标
- ✅ Actuator 端点暴露：`/actuator/prometheus`
- 📝 **生产建议**：集成 Grafana Dashboard

#### 5. **JWT 安全认证**
- ✅ Token 生成、验证、刷新
- ✅ Spring Security 集成
- ✅ 无状态认证（Stateless）
- ✅ 公开接口和受保护接口分离
- 📝 **生产建议**：密钥使用配置中心管理

---

### 🚀 生产环境部署步骤

#### 环境准备

```bash
# 1. 安装必备服务
# Java 17+
java -version

# Maven 3.6+
mvn -version

# Redis（可选）
sudo apt-get install redis-server
sudo systemctl start redis

# PostgreSQL（生产数据库）
sudo apt-get install postgresql
sudo systemctl start postgresql
```

#### 数据库配置

```sql
-- 创建 PostgreSQL 数据库
CREATE DATABASE ai_agent_prod;
CREATE USER ai_agent_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE ai_agent_prod TO ai_agent_user;
```

#### 应用配置

创建 `application-prod.yml`：

```yaml
# 生产环境配置
spring:
  # 数据库配置
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_agent_prod
    username: ai_agent_user
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  # JPA 配置
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # 生产环境禁止自动建表
    show-sql: false
    properties:
      hibernate:
        format_sql: false
  
  # Redis 配置
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      timeout: 5000
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 3000

# JWT 配置
jwt:
  secret: ${JWT_SECRET}  # 从环境变量读取，至少 256 bit
  expiration: 86400

# 向量存储
ai:
  embedding:
    type: filesystem
    filesystem-path: /data/ai-agent/embeddings
    auto-save-interval: 300

# 日志配置
logging:
  level:
    root: INFO
    com.yourcompany: INFO
    dev.langchain4j: WARN
  file:
    name: /var/log/ai-agent/application.log
    max-size: 100MB
    max-history: 30
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info,metrics
  endpoint:
    health:
      show-details: when-authorized
  server:
    port: 8081  # 监控端口分离
```

#### 启动应用

```bash
# 设置环境变量
export DB_PASSWORD=your_secure_password
export REDIS_PASSWORD=your_redis_password
export JWT_SECRET=your_256_bit_secret_key_here_must_be_long_enough
export DASHSCOPE_API_KEY=your_api_key

# 打包
mvn clean package -DskipTests

# 启动
java -jar \
  -Dspring.profiles.active=prod \
  target/langchain4j-agent-demo-1.0.0.jar
```

#### Docker 部署（推荐）

创建 `Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/langchain4j-agent-demo-1.0.0.jar app.jar

# 创建数据目录
RUN mkdir -p /data/ai-agent/embeddings /var/log/ai-agent

VOLUME ["/data/ai-agent", "/var/log/ai-agent"]

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
    volumes:
      - ./data:/data/ai-agent
      - ./logs:/var/log/ai-agent
    depends_on:
      - postgres
      - redis
    restart: always

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=ai_agent_prod
      - POSTGRES_USER=ai_agent_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: always

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    restart: always

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    ports:
      - "9090:9090"
    restart: always

  grafana:
    image: grafana/grafana:latest
    volumes:
      - grafana_data:/var/lib/grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
    restart: always

volumes:
  postgres_data:
  redis_data:
  prometheus_data:
  grafana_data:
```

启动：

```bash
docker-compose up -d
```

---

## 🔧 生产环境进一步优化建议

### 高优先级（必须实施）

#### 1. **向量数据库升级**
当前使用文件系统存储，生产环境强烈建议使用专业向量数据库：

**推荐方案：Milvus**
```xml
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.3.0</version>
</dependency>
```

优势：
- 分布式架构，支持海量向量数据
- 支持多种索引类型（IVF_FLAT、HNSW、IVF_PQ）
- 实时检索，毫秒级响应
- 数据持久化和备份

**替代方案**：
- Qdrant：Rust 实现，性能优异
- Pinecone：托管服务，零运维
- Weaviate：支持混合搜索

#### 2. **数据库连接池优化**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # 根据并发量调整
      minimum-idle: 5              # 保持最小连接数
      connection-timeout: 30000    # 连接超时
      idle-timeout: 600000         # 空闲连接回收
      max-lifetime: 1800000        # 连接最大生命周期
      validation-timeout: 5000     # 连接验证超时
```

#### 3. **Redis 高可用**
```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes: host1:26379,host2:26379,host3:26379
      password: ${REDIS_PASSWORD}
```

或 Redis Cluster：
```yaml
spring:
  data:
    redis:
      cluster:
        nodes: host1:6379,host2:6379,host3:6379
        max-redirects: 3
```

#### 4. **日志系统增强**
集成 ELK Stack（Elasticsearch + Logstash + Kibana）：

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

`logback-spring.xml`：
```xml
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>localhost:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

#### 5. **API 限流增强**
使用 Bucket4j 实现精细限流：

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.7.0</version>
</dependency>
```

```java
@Configuration
public class RateLimitConfig {
    @Bean
    public Filter rateLimitFilter() {
        return new Bucket4jSpringBootFilter();
    }
}
```

---

### 中优先级（建议实施）

#### 6. **配置中心集成**
使用 Spring Cloud Config 或 Nacos：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

优势：
- 配置集中管理
- 动态刷新（无需重启）
- 配置版本控制
- 多环境隔离

#### 7. **分布式追踪**
集成 SkyWalking 或 Zipkin：

```bash
# SkyWalking Agent
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=ai-agent \
     -Dskywalking.collector.backend_service=localhost:11800 \
     -jar app.jar
```

#### 8. **异步处理优化**
使用消息队列处理耗时任务：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

适用场景：
- 知识库文档批量导入
- Embedding 向量批量生成
- 学习经验异步处理

#### 9. **健康检查增强**
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // 检查数据库连接
        // 检查 Redis 连接
        // 检查向量存储状态
        // 检查外部 API 可用性
        return Health.up().build();
    }
}
```

#### 10. **数据备份策略**
```bash
# PostgreSQL 定时备份
0 2 * * * pg_dump ai_agent_prod > /backup/db_$(date +\%Y\%m\%d).sql

# Redis 备份
0 3 * * * redis-cli BGSAVE

# 向量存储备份
0 4 * * * tar -czf /backup/embeddings_$(date +\%Y\%m\%d).tar.gz /data/ai-agent/embeddings
```

---

### 低优先级（可选优化）

#### 11. **多模型支持**
支持多个 LLM 模型，实现智能路由：

```java
public class ModelRouter {
    public ChatLanguageModel selectModel(String taskType) {
        return switch (taskType) {
            case "code-generation" -> qwenMaxModel;
            case "code-review" -> gpt4Model;
            case "simple-question" -> qwenTurboModel;  // 成本更低
            default -> defaultModel;
        };
    }
}
```

#### 12. **响应式编程**
使用 Spring WebFlux 提升并发性能：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

#### 13. **GraphQL API**
提供更灵活的数据查询：

```xml
<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphql-spring-boot-starter</artifactId>
</dependency>
```

#### 14. **前端 Dashboard**
开发管理界面：
- 知识库文档管理
- 技能管理
- Token 使用监控
- 学习经验审核
- 系统配置管理

技术栈推荐：React + Ant Design + ECharts

#### 15. **测试覆盖**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

测试策略：
- 单元测试（覆盖率 > 80%）
- 集成测试（Testcontainers）
- 性能测试（JMeter/Gatling）
- 压力测试

---

## 📊 监控 Dashboard 示例

### Grafana Dashboard 配置

导入以下指标面板：

1. **系统监控**
   - JVM 内存使用
   - CPU 使用率
   - 线程池状态
   - GC 频率和时间

2. **业务监控**
   - AI 请求 QPS
   - 平均响应时间
   - Token 使用趋势
   - 知识库检索命中率
   - 错误率

3. **基础设施监控**
   - 数据库连接池使用率
   - Redis 命中率
   - 磁盘使用量
   - 网络 IO

---

## 📝 开发计划

### 已完成 ✅
- [x] 持久化向量存储（文件系统）
- [x] 数据库集成（H2 + JPA）
- [x] Redis 缓存优化
- [x] Prometheus 监控集成
- [x] JWT 安全认证
- [x] 知识库文档录入

### 进行中 🚧
- [ ] PostgreSQL 生产适配
- [ ] Grafana Dashboard 配置
- [ ] API 文档（Swagger/OpenAPI）
- [ ] 单元测试覆盖

### 规划中 📋
- [ ] 支持更多 LLM 模型（GPT-4、Claude 等）
- [ ] MCP 协议完整集成
- [ ] 代码执行沙箱
- [ ] Git 集成（代码提交、PR 生成）
- [ ] 多 Agent 协作
- [ ] 可视化 Dashboard
- [ ] 向量数据库升级（Milvus/Qdrant）
- [ ] 分布式追踪集成

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
