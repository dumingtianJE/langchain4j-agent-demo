import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          ElMessage.error('未授权，请登录')
          localStorage.removeItem('token')
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求错误，未找到该资源')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络错误，请检查后端服务是否启动')
    } else {
      ElMessage.error('请求配置错误')
    }
    return Promise.reject(error)
  }
)

// ==================== AI 对话 (AiChatController /api/ai) ====================
export const aiChat = {
  // 同步对话
  chat: (message, codeContext) =>
    api.post('/ai/chat', { message, codeContext }),

  // SSE 流式对话（返回 EventSource URL）
  getStreamUrl: (message, codeContext) => {
    const params = new URLSearchParams({ message })
    if (codeContext) params.append('codeContext', codeContext)
    return `/api/ai/chat/stream?${params.toString()}`
  },

  // 健康检查
  health: () => api.get('/ai/health')
}

// ==================== AI 编程 Agent (/api/ai-programming-agent) ====================
export const aiProgramming = {
  // 执行编程任务
  executeTask: (task, context, userId) =>
    api.post('/ai-programming-agent/execute', { task, context, userId }),

  // 代码审查
  reviewCode: (code, language) =>
    api.post('/ai-programming-agent/review-code', { code, language }),

  // 技术问题解答
  answerQuestion: (question) =>
    api.post('/ai-programming-agent/answer-question', { question }),

  // 生成文档
  generateDoc: (projectInfo, docType) =>
    api.post('/ai-programming-agent/generate-doc', { projectInfo, docType })
}

// ==================== 知识库 (/api/ai-programming-agent/knowledge) ====================
export const knowledge = {
  // 添加文档
  addDocument: (document) =>
    api.post('/ai-programming-agent/knowledge/add', document),

  // 检索文档
  search: (query, maxResults = 5) =>
    api.get(`/ai-programming-agent/knowledge/search?query=${encodeURIComponent(query)}&maxResults=${maxResults}`),

  // 获取统计
  getStats: () =>
    api.get('/ai-programming-agent/knowledge/stats')
}

// ==================== 技能 (/api/ai-programming-agent/skills) ====================
export const skills = {
  // 搜索技能
  search: (query) =>
    api.get(`/ai-programming-agent/skills/search?query=${encodeURIComponent(query)}`),

  // 获取所有技能
  getAll: () =>
    api.get('/ai-programming-agent/skills/all'),

  // 添加技能
  add: (skill) =>
    api.post('/ai-programming-agent/skills/add', skill)
}

// ==================== 自学习 (/api/ai-programming-agent/learning) ====================
export const learning = {
  // 记录反馈
  recordFeedback: (experience) =>
    api.post('/ai-programming-agent/learning/feedback', experience),

  // 学习新知识
  learnKnowledge: (knowledge, category) =>
    api.post(`/ai-programming-agent/learning/learn?knowledge=${encodeURIComponent(knowledge)}&category=${encodeURIComponent(category)}`),

  // 获取学习报告
  getReport: () =>
    api.get('/ai-programming-agent/learning/report')
}

// ==================== AI 监管 (/api/ai-programming-agent/supervisor) ====================
export const supervisor = {
  // Token 使用统计
  getStats: () =>
    api.get('/ai-programming-agent/supervisor/stats'),

  // 监管报告
  getReport: () =>
    api.get('/ai-programming-agent/supervisor/report'),

  // 警报日志
  getAlerts: (limit = 20) =>
    api.get(`/ai-programming-agent/supervisor/alerts?limit=${limit}`)
}

// ==================== Agent 控制器 (/api/agent) ====================
export const agent = {
  // 智能客服
  supportChat: (message) =>
    api.post('/agent/support/chat', null, { params: { message } }),

  // 订单管理
  orderChat: (message) =>
    api.post('/agent/order/chat', null, { params: { message } }),

  // 健康检查
  health: () =>
    api.get('/agent/health')
}

// ==================== 认证 (/api/auth) ====================
export const auth = {
  // 登录
  login: (username, password) =>
    api.post('/auth/login', { username, password }),

  // 刷新 Token
  refresh: (token) =>
    api.post('/auth/refresh', { token }),

  // 验证 Token
  validate: (token) =>
    api.post('/auth/validate', { token })
}

// ==================== 项目管理 (/api/project-management) ====================
export const projectManagement = {
  // 注册项目
  register: (data) =>
    api.post('/project-management/register', data),

  // 项目列表
  getAllProjects: () =>
    api.get('/project-management/projects'),

  // 项目详情
  getProject: (projectId) =>
    api.get(`/project-management/projects/${projectId}`),

  // 生成代码
  generateCode: (projectId, requirement) =>
    api.post(`/project-management/projects/${projectId}/generate-code`, { requirement }),

  // 优化代码
  optimizeCode: (projectId, existingCode, optimizationGoal) =>
    api.post(`/project-management/projects/${projectId}/optimize-code`, { existingCode, optimizationGoal }),

  // 验证代码
  validateCode: (projectId, code) =>
    api.post(`/project-management/projects/${projectId}/validate-code`, { code }),

  // 添加代码示例
  addCodeExample: (projectId, example) =>
    api.post(`/project-management/projects/${projectId}/code-examples`, example),

  // 获取代码示例
  getCodeExamples: (projectId) =>
    api.get(`/project-management/projects/${projectId}/code-examples`),

  // 代码生成上下文
  getContext: (projectId) =>
    api.get(`/project-management/projects/${projectId}/context`),

  // 搜索项目知识库
  searchKnowledge: (projectId, query, maxResults = 5) =>
    api.get(`/project-management/projects/${projectId}/search?query=${encodeURIComponent(query)}&maxResults=${maxResults}`),

  // 跨项目检索
  searchAcrossProjects: (query, projectIds, maxResults = 5) =>
    api.post('/project-management/search-across-projects', { query, projectIds, maxResults })
}

// ==================== 代码文件 (/api/code) ====================
export const codeFile = {
  getWorkspace: () => api.get('/code/workspace'),
  loadTree: (path = '.') =>
    api.get(`/code/tree?path=${encodeURIComponent(path)}`),
  read: (path) =>
    api.get(`/code/read?path=${encodeURIComponent(path)}`),
  write: (path, content, description) =>
    api.post('/code/write', { path, content, description })
}

// ==================== Spring Boot Actuator（路径在 /actuator，不在 /api 下） ====================
const actuatorApi = axios.create({
  baseURL: '',
  timeout: 10000
})

export const actuator = {
  health: () => actuatorApi.get('/actuator/health').catch(() => ({ status: 'UNKNOWN' })),
  info: () => actuatorApi.get('/actuator/info').catch(() => ({})),
  metrics: () => actuatorApi.get('/actuator/metrics').catch(() => ({ names: [] }))
}

export default api
