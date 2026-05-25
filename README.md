# LangChain4j Agent Demo

Spring Boot + LangChain4j 项目模板，包含完整的 Agent 架构示例和可视化前端界面。

## 项目结构

```
langchain4j-agent-demo/
├── src/main/java/com/yourcompany/langchain4j/  # 后端源代码
│   ├── LangChain4jAgentDemoApplication.java    # 启动类
│   ├── agent/                                   # Agent 接口
│   ├── config/                                  # 配置类
│   ├── controller/                              # REST 控制器
│   ├── tool/                                    # 工具类
│   └── ...
├── frontend/                                    # 前端项目 (Vue 3)
│   ├── src/
│   │   ├── api/                                # API 接口
│   │   ├── router/                             # 路由配置
│   │   ├── views/                              # 页面组件
│   │   └── ...
│   └── ...
├── QUICK_START.md                               # 快速启动指南
├── pom.xml                                      # Maven 配置
└── README.md                                    # 本文件
```

## 快速开始

### 方式一：使用可视化前端（推荐）

#### 1. 启动后端服务

```bash
mvn spring-boot:run
```

#### 2. 启动前端服务（新终端窗口）

```bash
cd frontend
npm install  # 首次运行
npm run dev
```

#### 3. 访问应用

打开浏览器访问：**http://localhost:3000**

前端提供以下功能：
- 💬 AI 对话界面
- 📝 代码编辑器
- 📚 知识库管理
- ⭐ 技能管理

### 方式二：直接使用 API

#### 1. 修改配置

编辑 `src/main/resources/application.yml`:
```yaml
langchain4j.open-ai.chat-model.api-key=你的API密钥
```

#### 2. 运行项目

```bash
mvn spring-boot:run
```

#### 3. 测试 API

```bash
# 客服对话
curl -X POST "http://localhost:8080/api/agent/support/chat?message=你好"

# 订单查询
curl -X POST "http://localhost:8080/api/agent/order/chat?message=查询订单ORD-123456"

# 健康检查
curl http://localhost:8080/api/agent/health
```

## 核心功能

### 1. Agent 定义

使用 `@AiService` 注解定义 AI Agent:

```java
@AiService
public interface CustomerSupportAgent {
    @SystemMessage("你是一个专业的智能客服助手...")
    String chat(@UserMessage String message);
}
```

### 2. Tool 工具

使用 `@Tool` 注解定义工具:

```java
@Tool("查询订单详细信息")
public String getOrderDetails(String orderId) {
    // 业务逻辑
}
```

### 3. 多 Agent 协作

项目中包含两个示例 Agent:
- **CustomerSupportAgent**: 处理客户咨询
- **OrderManagementAgent**: 处理订单管理(集成 OrderTool)

### 4. REST API

提供标准 REST 接口,方便前端或其他系统集成。

## 扩展开发

### 添加新的 Agent

1. 在 `agent` 包创建接口
2. 添加 `@AiService` 注解
3. 定义 `@SystemMessage` 和方法

### 添加新的 Tool

1. 在 `tool` 包创建类
2. 添加 `@Component` 和 `@Slf4j`
3. 在方法上添加 `@Tool` 注解

### 配置多个 Agent

在 `LangChain4jConfig` 中配置不同的 ChatMemory 策略。

## 技术栈

- **Spring Boot**: 3.4.2
- **LangChain4j**: 1.15.0-beta25
- **Java**: 17
- **LLM**: 通义千问 qwen-max (通过 OpenAI 兼容接口)

## 注意事项

1. API 密钥请妥善保管,不要提交到 Git
2. 生产环境建议使用环境变量管理配置
3. 工具方法要做好异常处理和日志记录
4. Agent 的 SystemMessage 要根据实际业务调整

## 许可证

MIT License
