# 🚀 快速启动指南

## 项目概览

本项目包含两个部分：
- **后端**: Spring Boot + LangChain4j AI 编程助手
- **前端**: Vue 3 + Element Plus 可视化操作界面

## 环境要求

### 后端
- JDK 17+
- Maven 3.6+
- (可选) MySQL 8.0+
- (可选) Redis 6.0+
- (可选) Milvus 向量数据库

### 前端
- Node.js 20.x
- npm 10.x+

## 启动步骤

### 1. 启动后端服务

```bash
# 进入后端项目目录
cd D:\workspace\langchain4j-agent-demo

# 编译项目
mvn clean install

# 启动后端服务
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 2. 启动前端服务

打开**新的终端窗口**：

```bash
# 进入前端项目目录
cd D:\workspace\langchain4j-agent-demo\frontend

# 安装依赖（首次运行）
npm install

# 启动前端开发服务器
npm run dev
```

前端服务将在 http://localhost:3000 启动

### 3. 访问应用

打开浏览器访问：**http://localhost:3000**

## 功能模块

### 📱 主要功能页面

1. **首页** (`/`)
   - 功能导航
   - 快速入口

2. **AI 对话** (`/chat`)
   - 与 AI 助手对话
   - 获取编程建议
   - 代码生成

3. **代码编辑器** (`/code-editor`)
   - 文件树浏览
   - 在线代码编辑
   - AI 辅助编程
   - 文件保存

4. **知识库管理** (`/knowledge`)
   - 添加/编辑文档
   - 搜索知识库
   - 分类管理

5. **技能管理** (`/skills`)
   - 技能添加/编辑
   - 技能等级评定
   - 使用统计

## 项目结构

```
langchain4j-agent-demo/
├── src/                      # 后端源代码
│   └── main/java/...
├── frontend/                 # 前端项目
│   ├── src/
│   │   ├── api/             # API 接口
│   │   ├── router/          # 路由配置
│   │   ├── views/           # 页面组件
│   │   ├── App.vue          # 根组件
│   │   └── main.js          # 入口
│   ├── package.json
│   └── vite.config.js
├── pom.xml                   # Maven 配置
└── README.md
```

## 开发调试

### 后端开发
- 修改 Java 代码后需要重启服务
- 使用 Spring Boot DevTools 可实现热重载

### 前端开发
- Vite 支持热模块替换 (HMR)
- 修改代码后浏览器自动刷新

### API 调试
- 前端配置了代理，自动转发到后端
- 代理配置：`frontend/vite.config.js`

## 常见问题

### Q: 前端无法连接后端？
A: 确保后端服务运行在 http://localhost:8080

### Q: 端口冲突？
A: 修改 `frontend/vite.config.js` 中的端口配置

### Q: npm install 失败？
A: 检查 Node.js 版本，建议使用 20.x

### Q: 后端启动失败？
A: 检查端口 8080 是否被占用，检查数据库配置

## 生产部署

### 后端打包
```bash
mvn clean package -DskipTests
java -jar target/langchain4j-agent-demo-0.0.1-SNAPSHOT.jar
```

### 前端构建
```bash
cd frontend
npm run build
```

构建产物在 `frontend/dist/` 目录，可部署到 Nginx 或其他 Web 服务器。

## 技术栈

### 后端
- Spring Boot 3.x
- LangChain4j
- Spring Data JPA
- Redis
- Milvus
- JWT 安全认证

### 前端
- Vue 3 (Composition API)
- Vite 5
- Element Plus
- Vue Router
- Axios

## 许可证

MIT

## 联系

如有问题，请提交 Issue。
