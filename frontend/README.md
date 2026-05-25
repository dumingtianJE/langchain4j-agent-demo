# AI 编程助手 - 前端项目

基于 Vue 3 + Element Plus 的 AI 编程助手可视化操作界面。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite 5** - 下一代前端构建工具
- **Element Plus** - Vue 3 组件库
- **Vue Router** - 官方路由管理器
- **Axios** - HTTP 客户端

## 功能特性

### 1. AI 对话
- 💬 与 AI 助手进行实时对话
- 📝 获取编程建议和代码生成
- 🎨 支持代码高亮显示
- 🗑️ 清空对话历史

### 2. 代码编辑器
- 📂 文件树浏览
- ✏️ 在线代码编辑
- 💾 文件保存功能
- 🤖 AI 辅助编程（右侧面板）

### 3. 知识库管理
- 📚 文档添加和编辑
- 🔍 知识库搜索
- 🗂️ 分类管理
- 🗑️ 文档删除

### 4. 技能管理
- ⭐ 技能添加和编辑
- 📊 技能等级评定
- 📈 使用统计查看
- 🎯 技能启用/禁用

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口
│   │   └── index.js      # Axios 配置
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── views/            # 页面组件
│   │   ├── Home.vue      # 首页
│   │   ├── ChatView.vue  # AI 对话
│   │   ├── CodeEditorView.vue  # 代码编辑器
│   │   ├── KnowledgeView.vue   # 知识库
│   │   └── SkillsView.vue      # 技能管理
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── vite.config.js        # Vite 配置
└── package.json
```

## API 配置

前端默认代理配置在 `vite.config.js`：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

确保后端服务运行在 `http://localhost:8080`

## 开发说明

### 添加新页面

1. 在 `src/views/` 创建 Vue 组件
2. 在 `src/router/index.js` 添加路由配置
3. 在 `src/App.vue` 添加菜单项

### API 调用

使用 `src/api/index.js` 中的 `api` 对象：

```javascript
import api from '../api'

// GET 请求
const data = await api.get('/endpoint')

// POST 请求
const result = await api.post('/endpoint', { data })
```

## 后端依赖

本前端项目需要配合后端使用：
- **后端地址**: http://localhost:8080
- **API 路径**: /api/*

请确保后端服务已启动并正常运行。

## 许可证

MIT
