# 🔌 前后端 AI 接口对接说明

## ✅ 对接状态：已完成

前端已经成功接入后端 AI 服务！

## 📋 接口配置

### 前端配置

**API 基础路径**: `/api` (相对路径)
**Vite 代理**: 自动转发到 `http://localhost:8080`

```javascript
// frontend/src/api/index.js
const api = axios.create({
  baseURL: '/api',  // 相对路径，通过 Vite 代理
  timeout: 30000
})
```

### 后端接口

**新增统一 AI 聊天接口**: `/api/ai/chat`

```java
// AiChatController.java
@PostMapping("/chat")
public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request)
```

## 🔗 完整调用链路

```
用户输入 (前端)
    ↓
ChatView.vue → sendMessage()
    ↓
api.post('/ai/chat', { message, codeContext })
    ↓
Vite Proxy (localhost:3000 → localhost:8080)
    ↓
AiChatController.chat()
    ↓
AiProgrammingAgent.answerTechnicalQuestion()
    ↓
LangChain4j + LLM (通义千问)
    ↓
AI 回复返回前端
```

## 📡 接口详情

### 1. AI 对话接口

**请求**:
```http
POST /api/ai/chat
Content-Type: application/json

{
  "message": "如何创建 Spring Boot REST API?",
  "codeContext": "可选的代码上下文",
  "userId": "可选的用户ID"
}
```

**响应**:
```json
{
  "success": true,
  "reply": "创建 Spring Boot REST API 的步骤如下...",
  "timestamp": 1716624000000
}
```

### 2. 健康检查接口

**请求**:
```http
GET /api/ai/health
```

**响应**:
```json
{
  "success": true,
  "status": "AI Chat Service is running",
  "timestamp": 1716624000000
}
```

## 🚀 启动步骤

### 1. 启动后端服务

```bash
cd D:\workspace\langchain4j-agent-demo
mvn spring-boot:run
```

**验证后端启动成功**:
```bash
curl http://localhost:8080/api/ai/health
```

### 2. 启动前端服务

```bash
cd frontend
npm run dev
```

前端将运行在: **http://localhost:3000** (或 3001)

### 3. 测试 AI 对话

1. 打开浏览器访问 http://localhost:3000
2. 点击左侧菜单 "AI 对话"
3. 在输入框输入问题，例如：
   - "如何创建 Vue 3 组件？"
   - "Spring Boot 如何配置数据库？"
   - "解释一下 RESTful API 设计原则"
4. 按 `Ctrl+Enter` 或点击"发送"按钮
5. 等待 AI 回复（通常 2-5 秒）

## 🎯 功能特性

### ✅ 已实现

1. **智能对话**
   - 技术问题解答
   - 代码生成建议
   - 最佳实践推荐

2. **代码上下文**
   - 支持发送代码片段
   - AI 基于上下文回答
   - 代码审查建议

3. **错误处理**
   - 网络错误提示
   - 服务不可用提醒
   - 友好的错误消息

4. **用户体验**
   - 加载状态显示
   - 消息历史滚动
   - 代码格式化显示

## 🔧 配置说明

### Vite 代理配置

```javascript
// frontend/vite.config.js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
      ws: true
    }
  }
}
```

### 后端依赖注入

```java
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    
    private final AiProgrammingAgent aiProgrammingAgent;
    // ...
}
```

## 📊 接口对比

| 功能 | 旧接口 | 新接口 |
|------|--------|--------|
| 客服对话 | `/api/agent/support/chat` | ✅ 保留 |
| 订单管理 | `/api/agent/order/chat` | ✅ 保留 |
| **AI 编程** | ❌ 无 | ✅ `/api/ai/chat` |
| 代码审查 | `/api/ai-programming-agent/review-code` | ✅ 保留 |
| 知识库 | `/api/ai-programming-agent/knowledge/*` | ✅ 保留 |

## 🐛 常见问题

### Q1: 前端提示"网络错误，请检查后端服务是否启动"

**原因**: 后端服务未启动或端口不对

**解决**:
```bash
# 检查后端是否运行
curl http://localhost:8080/api/ai/health

# 启动后端
mvn spring-boot:run
```

### Q2: 代理错误

**原因**: Vite 代理配置问题

**解决**: 检查 `frontend/vite.config.js` 代理配置

### Q3: CORS 跨域错误

**原因**: 不应该出现，因为使用了代理

**解决**: 确保前端使用相对路径 `/api`，而不是绝对路径

### Q4: AI 回复慢

**原因**: LLM API 调用需要时间

**解决**: 正常现象，通常 2-5 秒，复杂问题可能更长

## 📝 开发调试

### 查看代理日志

前端终端会显示代理请求：
```
代理请求: POST /api/ai/chat
```

### 查看后端日志

后端会显示：
```
收到 AI 对话请求: 如何创建 Spring Boot REST API?
```

### 浏览器开发者工具

1. 打开 F12 开发者工具
2. 切换到 Network 标签
3. 发送消息
4. 查看 `/api/ai/chat` 请求详情

## 🎨 前端界面

### AI 对话页面特性

- ✨ **科技感界面** - 赛博朋克风格
- 💬 **实时对话** - 流式交互体验
- 🎨 **代码高亮** - 自动格式化代码块
- 📜 **历史记录** - 完整对话历史
- ⚡ **快速响应** - 异步加载不阻塞

## 🔐 安全配置

### JWT 认证（可选）

```javascript
// 如果后端启用 JWT，前端会自动附加 Token
const token = localStorage.getItem('token')
if (token) {
  config.headers.Authorization = `Bearer ${token}`
}
```

### 请求超时

默认 30 秒超时，可在 `api/index.js` 修改：
```javascript
timeout: 30000  // 30秒
```

## 📈 下一步优化

- [ ] 流式输出（SSE）
- [ ] 对话历史持久化
- [ ] 多轮对话上下文
- [ ] 代码编辑器深度集成
- [ ] 文件上传支持

## ✅ 验证清单

- [x] 后端 AI 接口创建
- [x] 前端 API 配置更新
- [x] Vite 代理配置优化
- [x] 错误处理完善
- [x] 加载状态显示
- [x] 代码格式化
- [x] Git 提交

---

**最后更新**: 2026-05-25
**状态**: ✅ 对接完成，可正常使用
