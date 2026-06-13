<template>
  <div class="editor-container">
    <el-card class="editor-card">
      <template #header>
        <div class="card-header">
          <h2>📝 代码编辑器</h2>
          <div class="header-actions">
            <el-button @click="loadFile">
              <el-icon><FolderOpened /></el-icon>
              加载项目
            </el-button>
            <el-button type="success" @click="saveFile" :loading="isSaving">
              <el-icon><Download /></el-icon>
              保存
            </el-button>
            <el-button type="primary" @click="askAI">
              <el-icon><MagicStick /></el-icon>
              AI 辅助
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="panels-container" ref="panelsContainer">
        <!-- 文件树 -->
        <div class="panel file-tree-panel" :style="{ width: panelWidths.left + 'px' }">
          <el-card class="file-tree-card" v-loading="treeLoading">
            <template #header>
              <div class="tree-header">
                <span>文件目录</span>
                <el-button size="small" @click="refreshFiles" :disabled="!projectRoot">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </template>
            <div class="workspace-hint" v-if="workspaceRoot">
              工作区: {{ workspaceRoot }}
            </div>
            <el-tree
              v-if="fileTree.length"
              :data="fileTree"
              :props="treeProps"
              node-key="path"
              @node-click="handleFileClick"
              highlight-current
              default-expand-all
            />
            <el-empty v-else description="点击「加载项目」打开目录" :image-size="60" />
          </el-card>
        </div>
        
        <!-- 拖拽分隔线 -->
        <div class="panel-divider" @mousedown="startDrag($event, 'left')">
          <div class="divider-line"></div>
          <div class="divider-handle"></div>
          <div class="divider-line"></div>
        </div>
        
        <!-- 代码编辑区 -->
        <div class="panel code-editor-panel" :style="{ width: panelWidths.center + 'px' }">
          <el-card class="code-editor-card">
            <div class="editor-info">
              <span>当前文件: {{ currentFile || '未选择' }}</span>
              <el-tag v-if="isModified" type="warning">已修改</el-tag>
            </div>
            <el-input
              v-model="codeContent"
              type="textarea"
              :autosize="{ minRows: 20 }"
              placeholder="在此编辑代码..."
              class="code-textarea"
              @input="markModified"
            />
          </el-card>
        </div>
        
        <!-- 拖拽分隔线 -->
        <div class="panel-divider" @mousedown="startDrag($event, 'right')">
          <div class="divider-line"></div>
          <div class="divider-handle"></div>
          <div class="divider-line"></div>
        </div>
        
        <!-- AI 对话区 -->
        <div class="panel ai-chat-panel" :style="{ width: panelWidths.right + 'px' }">
          <el-card class="ai-chat-card">
            <template #header>
              <div class="chat-header">
                <span>AI 助手</span>
                <el-button 
                  v-if="chatMessages.length > 0"
                  text 
                  size="small" 
                  @click="clearChat"
                  title="清除对话记录"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </template>
            <!-- 快捷操作按钮 -->
            <div class="quick-actions">
              <div class="quick-action-card" @click="quickAction('logic')">
                <el-icon><MagicStick /></el-icon>
                解析该项目业务逻辑
              </div>
              <div class="quick-action-card" @click="quickAction('arch')">
                <el-icon><MagicStick /></el-icon>
                解析该项目业务架构
              </div>
            </div>
            <!-- 项目上下文缓存状态 -->
            <div class="cache-status-bar" v-if="projectRoot">
              <div class="cache-info">
                <el-icon :style="{ color: cacheStatus.cached ? '#67c23a' : '#909399' }">
                  <component :is="cacheStatus.cached ? 'CircleCheck' : 'Loading'" />
                </el-icon>
                <span class="cache-label">
                  {{ cacheStatus.cached ? '项目上下文已缓存' : '未缓存' }}
                </span>
                <el-tag v-if="cacheStatus.cached" size="small" type="success" effect="plain">
                  剩余{{ cacheStatus.remainingMinutes || 0 }}min
                </el-tag>
              </div>
              <el-button 
                size="small" 
                text 
                :loading="refreshingCache"
                @click="refreshContextCache"
                title="刷新项目上下文缓存"
              >
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
            <div class="chat-messages" ref="chatMessagesRef">
              <div 
                v-for="(msg, index) in chatMessages" 
                :key="index"
                :class="['chat-msg', msg.role]"
              >
                {{ msg.content }}
              </div>
            </div>
            <div class="chat-input">
              <el-input
                v-model="aiInput"
                type="textarea"
                :rows="3"
                placeholder="询问 AI..."
                @keyup.enter.ctrl="sendToAI"
              />
              <el-button 
                type="primary" 
                size="small" 
                @click="sendToAI"
                :loading="aiLoading"
                style="width: 100%; margin-top: 8px;"
              >
                发送
              </el-button>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>
    
    <!-- 加载项目对话框 -->
    <el-dialog v-model="loadDialogVisible" title="加载项目" width="520px">
      <!-- 模式切换 -->
      <div class="mode-switch">
        <span class="mode-label">加载模式：</span>
        <el-radio-group v-model="loadMode" size="default">
          <el-radio-button value="docker">Docker 部署</el-radio-button>
          <el-radio-button value="local">本地项目</el-radio-button>
        </el-radio-group>
      </div>
      <p class="dialog-tip" v-if="loadMode === 'docker'">
        Docker 部署模式下，项目运行在容器内。请输入容器内路径，通常使用
        <code>/app/workspace</code> 或 <code>.</code>（当前工作目录）。
      </p>
      <p class="dialog-tip" v-else>
        <template v-if="isDockerEnv">
          <strong style="color: #f56c6c;">注意：</strong>当前后端运行在 Docker 容器中，
          无法直接访问本机 Windows 路径。本地项目需要通过 Docker volumes 挂载，
          请使用挂载后的容器路径（如 <code>/app/workspace</code>）。
        </template>
        <template v-else>
          本地模式下，可直接输入本机任意项目目录的绝对路径，例如
          <code>D:\projects\my-app</code> 或 <code>/home/user/projects/my-app</code>。
        </template>
      </p>
      <el-input
        v-model="loadFilePath"
        :placeholder="getInputPlaceholder()"
        @keyup.enter="confirmLoadProject"
        @input="onPathInput"
      />
      <!-- 路径类型提示 -->
      <div class="path-hint" v-if="pathHint">
        <el-icon style="margin-right: 4px;"><WarningFilled /></el-icon>
        {{ pathHint }}
      </div>
      <template #footer>
        <el-button @click="loadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmLoadProject" :loading="treeLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { FolderOpened, Download, MagicStick, Refresh, WarningFilled, Delete, CircleCheck, Loading } from '@element-plus/icons-vue'
import { codeFile, aiChat, aiProgramming } from '../api'
import { ElMessage } from 'element-plus'

// ==================== 会话持久化（localStorage） ====================
const SK = 'code-editor:'
const saveLS = (key, val) => { try { localStorage.setItem(SK + key, typeof val === 'string' ? val : JSON.stringify(val)) } catch {} }
const loadLS = (key) => { try { const v = localStorage.getItem(SK + key); return v ? (v.startsWith('[') || v.startsWith('{') ? JSON.parse(v) : v) : null } catch { return null } }
const removeLS = (key) => { try { localStorage.removeItem(SK + key) } catch {} }

/** 保存当前会话状态 */
const saveSession = () => {
  saveLS('project-root', projectRoot.value)
  saveLS('load-mode', loadMode.value)
  saveLS('current-file', currentFile.value)
}

/** 保存 AI 聊天记录（最多保留最近 50 条） */
const saveChatHistory = () => {
  const msgs = chatMessages.value.slice(-50)
  saveLS('chat-messages', msgs)
}

/** 防抖保存编辑区草稿内容 */
let draftTimer = null
const saveDraft = () => {
  clearTimeout(draftTimer)
  draftTimer = setTimeout(() => {
    if (codeContent.value && currentFile.value) {
      saveLS('draft-file', currentFile.value)
      saveLS('draft-content', codeContent.value)
    }
  }, 1000)
}

/** 清除草稿 */
const clearDraft = () => {
  removeLS('draft-file')
  removeLS('draft-content')
}

// ==================== 状态变量 ====================
const codeContent = ref('')
const currentFile = ref('')
const isModified = ref(false)
const isSaving = ref(false)
const loadDialogVisible = ref(false)
const loadFilePath = ref('.')
const loadMode = ref('local')
const isDockerEnv = ref(false)
const pathHint = ref('')
const aiInput = ref('')
const aiLoading = ref(false)
const chatMessages = ref([])
const chatMessagesRef = ref(null)

// 项目上下文缓存状态
const cacheStatus = ref({ cached: false, remainingMinutes: 0 })
const refreshingCache = ref(false)

// 项目上下文缓存（用于 AI 分析时自动注入）
const projectTreeSummary = ref('')
const projectRootPath = ref('')

const fileTree = ref([])
const projectRoot = ref('')
const workspaceRoot = ref('')
const treeLoading = ref(false)

// ==================== 可拖拽分栏布局 ====================
const panelsContainer = ref(null)
const panelWidths = reactive({ left: 260, center: 700, right: 300 })
const MIN_PANEL_WIDTH = 180
const DIVIDER_WIDTH = 6  // 分隔线宽度 (px)

let dragState = null  // { side, startX, startWidths }

const startDrag = (e, side) => {
  e.preventDefault()
  dragState = {
    side,
    startX: e.clientX,
    startWidths: { ...panelWidths }
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

const onDrag = (e) => {
  if (!dragState) return
  const dx = e.clientX - dragState.startX
  const { startWidths, side } = dragState

  if (side === 'left') {
    // 拖动左侧分隔线：调整 left 和 center
    const newLeft = Math.max(MIN_PANEL_WIDTH, startWidths.left + dx)
    const delta = newLeft - startWidths.left
    const newCenter = startWidths.center - delta
    if (newCenter >= MIN_PANEL_WIDTH) {
      panelWidths.left = newLeft
      panelWidths.center = newCenter
    }
  } else {
    // 拖动右侧分隔线：调整 center 和 right
    const newCenter = Math.max(MIN_PANEL_WIDTH, startWidths.center + dx)
    const delta = newCenter - startWidths.center
    const newRight = startWidths.right - delta
    if (newRight >= MIN_PANEL_WIDTH) {
      panelWidths.center = newCenter
      panelWidths.right = newRight
    }
  }
}

const stopDrag = () => {
  dragState = null
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

/** 根据容器实际宽度初始化分栏比例 */
const initPanelWidths = () => {
  nextTick(() => {
    const container = panelsContainer.value
    if (!container) return
    const totalW = container.clientWidth
    const available = totalW - DIVIDER_WIDTH * 2  // 减去两条分隔线
    if (available <= 0) return
    // 默认比例：左 18% / 中 58% / 右 24%
    panelWidths.left = Math.round(available * 0.18)
    panelWidths.center = Math.round(available * 0.58)
    panelWidths.right = available - panelWidths.left - panelWidths.center
  })
}

const treeProps = {
  children: 'children',
  label: 'name'
}

// ==================== 初始化：恢复会话 ====================
onMounted(async () => {
  // 初始化分栏布局
  initPanelWidths()
  window.addEventListener('resize', initPanelWidths)

  try {
    const res = await codeFile.getWorkspace()
    workspaceRoot.value = res.workspaceRoot || ''
    isDockerEnv.value = workspaceRoot.value.startsWith('/') && workspaceRoot.value.includes('/app')
    if (isDockerEnv.value) {
      loadMode.value = 'docker'
      loadFilePath.value = '.'
    } else {
      loadMode.value = 'local'
      loadFilePath.value = workspaceRoot.value || '.'
    }
  } catch {
    loadFilePath.value = '.'
  }

  // 恢复上次的项目路径
  const savedRoot = loadLS('project-root')
  const savedMode = loadLS('load-mode')
  if (savedRoot) {
    loadMode.value = savedMode || loadMode.value
    loadFilePath.value = savedRoot
    // 自动加载项目文件树
    try {
      const res = await codeFile.loadTree(savedRoot)
      const data = res.data || {}
      const treeNode = data.tree
      fileTree.value = treeNode ? [toTreeNode(treeNode)] : []
      projectRoot.value = data.projectRoot || data.root || savedRoot
      workspaceRoot.value = data.projectRoot || data.absolutePath || savedRoot
      // 恢复会话时自动构建缓存
      buildContextCache(projectRoot.value)
    } catch {
      // 项目路径可能已失效，忽略
    }
  }

  // 恢复上次打开的文件
  const savedFile = loadLS('current-file')
  if (savedFile) {
    try {
      const res = await codeFile.read(savedFile)
      const data = res.data || {}
      codeContent.value = data.content || ''
      currentFile.value = data.path || savedFile
      isModified.value = false
    } catch {
      // 文件可能已不存在，尝试恢复草稿
      const draftFile = loadLS('draft-file')
      const draftContent = loadLS('draft-content')
      if (draftFile && draftContent) {
        currentFile.value = draftFile
        codeContent.value = draftContent
        isModified.value = true
      }
    }
  } else {
    // 没有保存的文件，但有草稿
    const draftFile = loadLS('draft-file')
    const draftContent = loadLS('draft-content')
    if (draftFile && draftContent) {
      currentFile.value = draftFile
      codeContent.value = draftContent
      isModified.value = true
    }
  }

  // 恢复 AI 聊天记录
  const savedChat = loadLS('chat-messages')
  if (savedChat && Array.isArray(savedChat)) {
    chatMessages.value = savedChat
    scrollToBottom()
  }

  // 预加载项目上下文（用于 AI 项目分析时自动注入）
  loadProjectContext()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', initPanelWidths)
  stopDrag()  // 清理拖拽事件
})

/** 从后端获取项目上下文摘要并缓存 */
const loadProjectContext = async () => {
  try {
    const res = await aiChat.getProjectContext()
    if (res.success) {
      projectRootPath.value = res.projectRoot || ''
      projectTreeSummary.value = res.treeSummary || ''
    }
    // 同时查询缓存状态
    await updateCacheStatus()
  } catch {
    // 项目上下文加载失败不影响其他功能
  }
}

/** 检测 Windows 风格路径（如 D:\... 或 C:/...） */
const isWindowsPath = (p) => /^[A-Za-z]:[/\\]/.test(p)

/** 输入框 placeholder */
const getInputPlaceholder = () => {
  if (loadMode.value === 'docker') return '例如: /app/workspace 或 .'
  if (isDockerEnv.value) return 'Docker 容器中请使用 /app/workspace'
  return '例如: D:\\projects\\my-app 或 /home/user/project'
}

/** 路径输入时检测并给出提示 */
const onPathInput = (val) => {
  pathHint.value = ''
  if (!val) return
  const trimmed = val.trim()
  // 在 Docker 环境中输入 Windows 路径
  if (isDockerEnv.value && isWindowsPath(trimmed) && loadMode.value === 'local') {
    pathHint.value = '检测到 Windows 路径，但后端在 Docker 容器中无法直接访问。请切换到「Docker 部署」模式。'
    return
  }
  // 在 Docker 模式下输入 Windows 路径
  if (isDockerEnv.value && isWindowsPath(trimmed) && loadMode.value === 'docker') {
    pathHint.value = 'Windows 路径无法在 Docker 容器中使用，请使用容器内路径如 /app/workspace'
    return
  }
  // 在非 Docker 环境中输入 Linux 绝对路径
  if (!isDockerEnv.value && trimmed.startsWith('/') && loadMode.value === 'docker') {
    pathHint.value = '检测到 Linux 绝对路径，建议切换到「本地项目」模式。'
    return
  }
}

const markModified = () => {
  isModified.value = true
  saveDraft()  // 自动保存草稿
}

const toTreeNode = (node) => {
  if (!node) return null
  return {
    name: node.name,
    path: node.path,
    type: node.type,
    children: node.type === 'directory' && node.children?.length
      ? node.children.map(toTreeNode).filter(Boolean)
      : undefined
  }
}

const loadFile = () => {
  // 打开对话框时，根据模式设置默认路径
  if (loadMode.value === 'docker') {
    loadFilePath.value = projectRoot.value || '.'
  } else {
    loadFilePath.value = projectRoot.value || workspaceRoot.value || '.'
  }
  loadDialogVisible.value = true
}

const confirmLoadProject = async () => {
  let path = loadFilePath.value?.trim() || '.'
  // 前端预校验：Docker 环境中拦截 Windows 路径
  if (isDockerEnv.value && isWindowsPath(path)) {
    ElMessage.warning('后端运行在 Docker 容器中，无法访问 Windows 本地路径。请使用容器内路径（如 /app/workspace），或将本地目录挂载到容器中。')
    return
  }
  treeLoading.value = true
  try {
    const res = await codeFile.loadTree(path)
    const data = res.data || {}
    const treeNode = data.tree
    fileTree.value = treeNode ? [toTreeNode(treeNode)] : []
    projectRoot.value = data.projectRoot || data.root || path
    workspaceRoot.value = data.projectRoot || data.absolutePath || path
    loadDialogVisible.value = false
    saveSession()  // 持久化项目路径
    ElMessage.success(`项目已加载: ${data.absolutePath || path}`)
    // 加载项目后自动构建上下文缓存（后续编程请求不再重复分析项目结构）
    buildContextCache(projectRoot.value)
  } catch (error) {
    ElMessage.error('加载项目失败: ' + (error.response?.data?.error || error.message))
  } finally {
    treeLoading.value = false
  }
}

const openFile = async (filePath) => {
  if (!filePath || filePath === '.') return
  treeLoading.value = true
  try {
    const res = await codeFile.read(filePath)
    const data = res.data || {}
    codeContent.value = data.content || ''
    currentFile.value = data.path || filePath
    isModified.value = false
    clearDraft()   // 打开新文件时清除旧草稿
    saveSession()  // 持久化当前文件路径
    ElMessage.success('文件已打开')
  } catch (error) {
    ElMessage.error('打开文件失败: ' + (error.response?.data?.error || error.message))
  } finally {
    treeLoading.value = false
  }
}

const saveFile = async () => {
  if (!currentFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  isSaving.value = true
  try {
    await codeFile.write(currentFile.value, codeContent.value, '通过前端编辑器保存')
    isModified.value = false
    clearDraft()  // 保存成功后清除草稿
    ElMessage.success('文件保存成功')
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.response?.data?.error || error.message))
  } finally {
    isSaving.value = false
  }
}

const refreshFiles = () => {
  if (!projectRoot.value) {
    ElMessage.warning('请先加载项目')
    return
  }
  loadFilePath.value = projectRoot.value
  confirmLoadProject()
}

const handleFileClick = (data) => {
  if (data.type === 'file' && data.path) {
    openFile(data.path)
  }
}

const askAI = () => {
  ElMessage.info('请在右侧 AI 助手面板中输入问题')
}

const sendToAI = async () => {
  if (!aiInput.value.trim() || aiLoading.value) return
  
  let userMessage = aiInput.value
  
  // 如果已加载项目，自动附加项目路径信息供AI使用
  if (projectRoot.value) {
    const pathInfo = `\n\n【当前项目路径】: ${projectRoot.value}`
    // 对于涉及项目分析类的请求，附加路径信息
    const projectKeywords = ['解析', '分析', '业务逻辑', '业务架构', '项目结构', 
      '代码结构', '模块', '技术栈', '依赖', '架构', '全量', '流程']
    if (projectKeywords.some(kw => userMessage.includes(kw))) {
      userMessage += pathInfo
    }
  }
  
  // 检测是否为复杂问题或适合编排器处理的请求（代码生成/审查/复合任务）
  if (isComplexQuestion(userMessage) || isOrchestratorCandidate(userMessage)) {
    await handleComplexQuestion(userMessage)
    return
  }
  
  chatMessages.value.push({ role: 'user', content: userMessage })
  saveChatHistory()  // 保存用户消息
  aiInput.value = ''
  aiLoading.value = true
  
  try {
    // 构建 codeContext：优先传代码内容，否则传项目路径+文件树摘要，让 AI 知道项目位置
    let context = codeContent.value?.trim() ? codeContent.value.substring(0, 8000) : null
    if (!context && projectRoot.value) {
      context = `【项目工作区路径】: ${projectRoot.value}`
      if (projectTreeSummary.value) {
        context += `\n\n【项目文件结构摘要】:\n${projectTreeSummary.value}`
      }
    }
    const response = await aiChat.chat(userMessage, context)
    chatMessages.value.push({
      role: 'assistant',
      content: response.reply || response.message || response.result || '收到回复'
    })
    saveChatHistory()  // 保存 AI 回复
    scrollToBottom()
  } catch (error) {
    let errMsg = '❌ 请求失败'
    if (error.response) {
      const data = error.response.data
      // 后端返回的业务错误信息
      if (data?.reply) {
        errMsg = data.reply
      } else if (data?.message) {
        errMsg = '❌ ' + data.message
      } else {
        errMsg = `❌ 请求失败 (${error.response.status})`
      }
        
      // 如果是工具调用序列错误，自动清除对话历史
      if (error.response.status === 400 && (errMsg.includes('tool_calls') || errMsg.includes('工具调用') || errMsg.includes('对话上下文异常'))) {
        ElMessage.warning('检测到对话历史异常，已自动清除。请重新发送消息。')
        chatMessages.value = []
        removeLS('chat-messages')
        scrollToBottom()
        aiLoading.value = false
        return
      }
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      errMsg = '❌ AI 响应超时，请稍后重试或缩短问题内容'
    } else if (error.request) {
      errMsg = '❌ 网络连接失败，请检查后端服务是否正常运行'
    } else {
      errMsg = '❌ ' + error.message
    }
    chatMessages.value.push({
      role: 'assistant',
      content: errMsg
    })
    saveChatHistory()
    scrollToBottom()
  } finally {
    aiLoading.value = false
  }
}

/** 检测是否为复杂问题 */
const isComplexQuestion = (message) => {
  // 1. 用户明确列出了多个要求（编号列表、多个问句）
  const hasNumberedList = /\n\s*\d+[.、）)]/.test(message) || /^\s*\d+[.、）)]/m.test(message)
  const hasMultipleQuestions = (message.match(/？/g) || []).length >= 2
  const hasMultipleItems = (message.match(/[；;]/g) || []).length >= 2
  
  // 2. 包含"全面/详细/深度"等复合分析词 + 分析动词
  const hasDeepAnalysis = 
    /全面|详细|深度|完整|综合|系统性/.test(message) &&
    /分析|评估|总结|梳理|解析|审查|规划|设计/.test(message)
  
  // 3. 涉及多个维度（逗号/顿号分隔的并列项 >= 3 个）
  const hasMultipleAspects = (() => {
    // 匹配 "包括：A、B、C" 或 "A，B，C" 这类结构
    const items = message.match(/[、，,]/g)
    return items && items.length >= 3
  })()
  
  // 4. 预定义的复杂关键词（覆盖常见场景）
  const complexKeywords = [
    '全面分析', '详细分析', '深度分析', '完整分析',
    '全面评估', '详细评估', '深度评估',
    '全面总结', '详细总结', '全面梳理',
    '综合分析', '系统性分析',
    '项目架构', '系统架构', '软件架构',
    '业务流程', '业务逻辑',
    '技术栈', '技术选型',
    '设计模式', '设计原则',
    '性能优化', '性能分析',
    '安全分析', '安全评估',
    '代码质量', '代码审查',
    '重构方案', '优化方案',
    '微服务设计', '迁移方案', '技术方案',
    '对比分析', '技术对比', '方案对比'
  ]
  const hasComplexKeyword = complexKeywords.some(kw => message.includes(kw))
  
  return hasNumberedList || hasMultipleQuestions || hasMultipleItems || 
         hasDeepAnalysis || hasMultipleAspects || hasComplexKeyword
}

/** 检测是否适合走 AgentOrchestrator 编排器（代码生成/审查/文档等意图） */
const isOrchestratorCandidate = (message) => {
  const orchestratorKeywords = [
    // 代码生成类
    '写代码', '编写代码', '生成代码', '实现代码', '代码实现',
    '帮我写', '写一个', '编写一个', '实现一个', '创建一个',
    '用java', '用python', '用go', '用rust', '用javascript',
    '实现功能', '添加功能', '新增功能', '开发功能',
    // 代码审查类
    '代码审查', '审查代码', 'review', '代码review',
    '代码质量', '检查代码', '代码问题', '代码缺陷',
    // 文档类
    '生成文档', '编写文档', '写文档', '技术文档',
    'api文档', '接口文档', '使用文档', '说明文档',
    // 复合意图
    '实现并审查', '开发并测试', '设计并实现'
  ]
  const lower = message.toLowerCase()
  return orchestratorKeywords.some(kw => lower.includes(kw))
}

/** 智能切分复杂问题 — 先让 AI 规划，再逐步执行 */
const splitComplexQuestion = async (message) => {
  // 第一步：尝试让 AI 智能规划子任务
  try {
    const projectHint = projectRoot.value ? `\n\n项目工作区路径：${projectRoot.value}` : ''
    const planPrompt = `你是一个任务规划专家。请将以下复杂问题拆分为 2~4 个独立的子任务，每个子任务用一句话描述要做什么。

要求：
1. 子任务之间要有逻辑顺序（先整体后细节）
2. 每个子任务应该是一次简短的 AI 问答能完成的
3. 最后一个子任务通常是汇总或建议
4. 严格按 JSON 数组格式返回，不要有其他文字

返回格式：
[{"title":"步骤标题","prompt":"具体要分析的内容"}]

用户的问题：
${message}${projectHint}`

    const response = await aiChat.chat(planPrompt, null)
    const reply = response.reply || response.message || response.result || ''
    
    // 尝试从 AI 回复中解析 JSON
    const tasks = parseTasksFromAI(reply)
    if (tasks && tasks.length >= 2) {
      return tasks
    }
  } catch (e) {
    console.warn('AI 规划失败，使用本地切分策略', e)
  }
  
  // 第二步：AI 规划失败时，使用本地启发式切分
  return localSplitQuestion(message)
}

/** 从 AI 回复中解析任务列表 */
const parseTasksFromAI = (reply) => {
  try {
    // 尝试直接解析
    let jsonStr = reply.trim()
    // 提取 JSON 数组（AI 可能在 JSON 外有额外文字）
    const match = jsonStr.match(/\[[\s\S]*\]/)
    if (match) {
      jsonStr = match[0]
    }
    const parsed = JSON.parse(jsonStr)
    if (Array.isArray(parsed) && parsed.length >= 2) {
      return parsed.map((item, i) => ({
        title: item.title || `步骤 ${i + 1}`,
        prompt: item.prompt || item.task || item.description || item.title
      }))
    }
  } catch (e) {
    console.warn('解析 AI 任务规划 JSON 失败', e)
  }
  return null
}

/** 本地启发式切分（作为 AI 规划的兜底） */
const localSplitQuestion = (message) => {
  const projectPath = projectRoot.value || '/app/workspace'
  // 预定义的切分规则（覆盖常见场景）
  const splitRules = [
    {
      pattern: /业务逻辑|业务流程/,
      tasks: [
        { title: '📊 分析项目结构', prompt: `请分析项目 ${projectPath} 的结构、技术栈和主要模块` },
        { title: '🔄 分析业务流程', prompt: `请分析项目 ${projectPath} 的核心业务流程和用户交互流程` },
        { title: '📦 分析数据流向', prompt: `请分析项目 ${projectPath} 的数据流向和数据存储方式` },
        { title: '🔗 分析模块关系', prompt: `请分析项目 ${projectPath} 各模块间的协作关系和调用链路` }
      ]
    },
    {
      pattern: /架构|系统架构|项目架构/,
      tasks: [
        { title: '🏗️ 系统分层', prompt: `请分析项目 ${projectPath} 的系统分层设计和各层职责` },
        { title: '⚙️ 技术选型', prompt: `请分析项目 ${projectPath} 的技术栈选型及技术决策` },
        { title: '🧩 模块划分', prompt: `请分析项目 ${projectPath} 的功能模块划分和职责边界` },
        { title: '✅ 架构评估', prompt: `请评估项目 ${projectPath} 的架构优缺点并提出改进建议` }
      ]
    },
    {
      pattern: /性能|优化/,
      tasks: [
        { title: '⚡ 性能分析', prompt: `请分析项目 ${projectPath} 的性能瓶颈` },
        { title: '🔍 问题定位', prompt: '请定位具体的性能问题点' },
        { title: '💡 优化方案', prompt: '请提供性能优化方案和最佳实践' },
        { title: '📈 改进建议', prompt: '请给出具体的实施建议和优先级' }
      ]
    },
    {
      pattern: /安全|安全性/,
      tasks: [
        { title: '🔒 安全分析', prompt: `请分析项目 ${projectPath} 的安全风险点` },
        { title: '🛡️ 漏洞评估', prompt: '请评估潜在的安全漏洞' },
        { title: '✅ 防护方案', prompt: '请提供安全防护方案' },
        { title: '📋 安全建议', prompt: '请给出安全最佳实践建议' }
      ]
    },
    {
      pattern: /代码质量|代码审查|重构/,
      tasks: [
        { title: '📝 代码分析', prompt: `请分析项目 ${projectPath} 的代码质量现状` },
        { title: '⚠️ 问题识别', prompt: '请识别代码中的问题和风险' },
        { title: '💡 改进方案', prompt: '请提供代码改进和重构方案' },
        { title: '📝 最佳实践', prompt: '请给出编码规范和最佳实践' }
      ]
    }
  ]
  
  for (const rule of splitRules) {
    if (rule.pattern.test(message)) {
      return rule.tasks
    }
  }
  
  // 通用兜底：按问题的结构自动拆分
  return generateGenericSplitTasks(message)
}

/** 通用兜底切分 — 适用于任何复杂问题 */
const generateGenericSplitTasks = (message) => {
  const tasks = []
  
  // 尝试从用户消息中提取编号列表项
  const numberedItems = message.match(/\d+[.、）)][^\d\n]+/g)
  if (numberedItems && numberedItems.length >= 2) {
    numberedItems.slice(0, 4).forEach((item, i) => {
      const cleanItem = item.replace(/^\d+[.、）)]\s*/, '').trim()
      tasks.push({
        title: `📌 分析：${cleanItem.substring(0, 15)}${cleanItem.length > 15 ? '...' : ''}`,
        prompt: `请针对以下方面进行详细分析：${cleanItem}`
      })
    })
    if (tasks.length >= 2) return tasks
  }
  
  // 尝试按分号/逗号分隔的并列项拆分
  const splitBySemicolon = message.split(/[；;]/).map(s => s.trim()).filter(s => s.length > 5)
  if (splitBySemicolon.length >= 2) {
    splitBySemicolon.slice(0, 4).forEach((item, i) => {
      tasks.push({
        title: `📌 第 ${i + 1} 部分`,
        prompt: `请针对以下方面进行详细分析：${item}`
      })
    })
    return tasks
  }
  
  // 最终兜底：三段式通用分析
  return [
    { title: '📋 总体概述', prompt: `请对以下问题提供总体概述和关键要点：${message}` },
    { title: '🔍 详细分析', prompt: `请对以下问题进行详细分析和深入探讨：${message}` },
    { title: '💡 建议与总结', prompt: `请对以下问题给出具体建议、最佳实践和改进方向：${message}` }
  ]
}

/** 处理复杂问题 — 委托给后端 AgentOrchestrator 编排器 */
const handleComplexQuestion = async (message) => {
  chatMessages.value.push({ role: 'user', content: message })
  saveChatHistory()
  aiInput.value = ''
  aiLoading.value = true

  // 先显示"编排中"提示
  chatMessages.value.push({
    role: 'assistant',
    content: '🧠 正在分析意图，规划编排流水线...'
  })
  scrollToBottom()

  try {
    // 构建 codeContext：优先传代码内容，否则传项目路径+文件树摘要
    let codeContext = codeContent.value?.trim() ? codeContent.value.substring(0, 8000) : null
    if (!codeContext && projectRoot.value) {
      codeContext = `【项目工作区路径】: ${projectRoot.value}`
      if (projectTreeSummary.value) {
        codeContext += `\n\n【项目文件结构摘要】:\n${projectTreeSummary.value}`
      }
    }

    const res = await aiProgramming.orchestrate(message, codeContext, 'web-user')

    // 移除"编排中"提示
    chatMessages.value.pop()

    if (res.success) {
      // 显示意图和流水线概览
      const pipelineOverview = res.pipeline
        ? res.pipeline.map((s, i) => `  ${i + 1}. ${s}`).join('\n')
        : ''
      chatMessages.value.push({
        role: 'assistant',
        content: `${res.intentLabel || '🤖 智能编排'}\n\n⚙️ 执行流水线：\n${pipelineOverview}\n\n⏱️ 总耗时：${((res.totalDurationMs || 0) / 1000).toFixed(1)}s`
      })
      saveChatHistory()
      scrollToBottom()

      // 逐步展示各步骤结果
      if (res.steps && res.steps.length > 0) {
        for (const step of res.steps) {
          const statusIcon = step.success ? '✅' : '⚠️'
          const durationStr = step.durationMs ? ` (${(step.durationMs / 1000).toFixed(1)}s)` : ''
          chatMessages.value.push({
            role: 'assistant',
            content: `${statusIcon} 步骤 ${step.stepNumber}：${step.stepName}${durationStr}\n\n${step.result || step.error || '（无输出）'}`
          })
          saveChatHistory()
          scrollToBottom()
        }
      }

      // 最终汇总
      chatMessages.value.push({
        role: 'assistant',
        content: `📋 **编排完成 — 最终结果**\n\n${res.finalResult || '所有步骤已执行完毕，请查看上方各步骤详细结果。'}`
      })
    } else {
      // 展示各步骤的失败详情
      const stepDetails = res.steps && res.steps.length > 0
        ? res.steps.map(s => {
            const icon = s.success ? '✅' : '❌'
            const info = s.error || '无输出'
            return `${icon} ${s.stepName}: ${info}`
          }).join('\n')
        : ''
      chatMessages.value.push({
        role: 'assistant',
        content: `⚠️ **编排执行失败**\n\n意图：${res.intentLabel || '未知'}\n\n各步骤状态：\n${stepDetails}\n\n错误：${res.error || '未知错误'}\n\n💡 建议：AI 服务响应可能超时，请稍后重试或简化您的请求。`
      })
    }
    saveChatHistory()
    scrollToBottom()

  } catch (error) {
    chatMessages.value.pop()  // 移除"编排中"提示
    const errData = error.response?.data
    const errMsg = errData?.error || errData?.reply || errData?.message || error.message
    
    // 如果有步骤信息，展示各步骤状态
    if (errData?.steps && errData.steps.length > 0) {
      const stepDetails = errData.steps.map(s => {
        const icon = s.success ? '✅' : '❌'
        const info = s.error || s.result?.substring(0, 100) || '无输出'
        return `${icon} ${s.stepName}: ${info}`
      }).join('\n')
      ElMessage.error('编排失败: ' + (errMsg || '未知错误'))
      chatMessages.value.push({
        role: 'assistant',
        content: `❌ **编排请求失败**\n\n意图：${errData.intentLabel || '未知'}\n\n各步骤状态：\n${stepDetails}\n\n错误详情：${errMsg || '未知错误'}\n\n💡 建议：可能是 AI 服务响应超时或网络连接不稳定，请稍后重试。`
      })
    } else {
      ElMessage.error('编排失败: ' + errMsg)
      chatMessages.value.push({
        role: 'assistant',
        content: '❌ 编排请求失败：' + errMsg + '\n\n💡 建议：可能是 AI 服务响应超时或网络连接不稳定，请稍后重试。'
      })
    }
    saveChatHistory()
    scrollToBottom()
  } finally {
    aiLoading.value = false
  }
}

/** 快捷操作：解析项目业务逻辑 / 架构 */
const quickAction = async (type) => {
  if (aiLoading.value) {
    ElMessage.warning('AI 正在处理中，请稍候...')
    return
  }
  
  // 检查是否已加载项目
  if (!projectRoot.value) {
    ElMessage.warning('请先加载项目')
    return
  }
  
  // 构造复杂问题消息，注入项目路径和文件树摘要
  const projectInfo = `\n\n【项目工作区路径】: ${projectRoot.value}`
  const treeInfo = projectTreeSummary.value ? `\n\n【项目文件结构摘要】:\n${projectTreeSummary.value}` : ''
  
  let message
  if (type === 'logic') {
    message = `请全面分析并解析当前项目的业务逻辑，包括：
1. 项目结构和技术栈
2. 核心业务流程
3. 数据流向
4. 各模块之间的协作关系${projectInfo}${treeInfo}`
  } else if (type === 'arch') {
    message = `请全面分析并解析当前项目的业务架构，包括：
1. 系统分层设计
2. 技术选型
3. 模块划分
4. 服务间通信方式
5. 整体架构的优缺点${projectInfo}${treeInfo}`
  }
  
  // 使用通用复杂问题处理机制
  aiInput.value = message
  await handleComplexQuestion(message)
}

const scrollToBottom = () => {
  setTimeout(() => {
    const container = chatMessagesRef.value || document.querySelector('.chat-messages')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  }, 100)
}

/** 清除 AI 对话记录 */
const clearChat = () => {
  chatMessages.value = []
  removeLS('chat-messages')
  ElMessage.success('对话记录已清除')
}

/** 构建项目上下文缓存（后台执行，不阻塞 UI） */
const buildContextCache = async (projectPath) => {
  try {
    await aiChat.buildProjectSummary(projectPath)
    // 构建成功后更新缓存状态
    await updateCacheStatus()
  } catch {
    // 缓存构建失败不影响其他功能
  }
}

/** 刷新项目上下文缓存 */
const refreshContextCache = async () => {
  refreshingCache.value = true
  try {
    await aiChat.refreshProjectSummary()
    await updateCacheStatus()
    ElMessage.success('项目上下文缓存已刷新')
  } catch {
    ElMessage.error('刷新缓存失败')
  } finally {
    refreshingCache.value = false
  }
}

/** 更新缓存状态显示 */
const updateCacheStatus = async () => {
  try {
    const res = await aiChat.getProjectSummaryStatus()
    if (res.success && res.cacheStatus) {
      cacheStatus.value = res.cacheStatus
    }
  } catch {
    // 状态查询失败不影响功能
  }
}
</script>

<style scoped>
.editor-container {
  padding: 30px;
  height: calc(100vh - 40px);
}

.editor-card {
  height: 100%;
  background: var(--card-bg);
  border: 1px solid var(--border-glow);
  backdrop-filter: blur(10px);
}

:deep(.el-card__header) {
  border-bottom: 1px solid var(--border-glow);
  background: rgba(5, 8, 17, 0.5);
}

/* ==================== 可拖拽分栏布局 ==================== */
.panels-container {
  display: flex;
  align-items: stretch;
  height: calc(100vh - 200px);
  gap: 0;
}

.panel {
  flex-shrink: 0;
  overflow: hidden;
}

.panel > :deep(.el-card) {
  height: 100%;
}

.panel-divider {
  width: 6px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: col-resize;
  gap: 2px;
  padding: 0 1px;
  transition: background 0.2s ease;
  z-index: 10;
}

.panel-divider:hover,
.panel-divider:active {
  background: rgba(0, 242, 254, 0.08);
}

.divider-line {
  flex: 1;
  width: 2px;
  background: var(--border-glow);
  border-radius: 1px;
  transition: background 0.2s ease;
}

.divider-handle {
  width: 6px;
  height: 32px;
  border-radius: 3px;
  background: var(--border-glow);
  transition: all 0.2s ease;
}

.panel-divider:hover .divider-line {
  background: var(--neon-blue);
  box-shadow: 0 0 6px rgba(0, 242, 254, 0.4);
}

.panel-divider:hover .divider-handle {
  background: var(--neon-blue);
  box-shadow: 0 0 10px rgba(0, 242, 254, 0.5);
  height: 48px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--neon-blue), var(--neon-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  gap: 12px;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.file-tree-card,
.code-editor-card,
.ai-chat-card {
  height: 100%;
  background: rgba(16, 22, 58, 0.4);
  border: 1px solid var(--border-glow);
  overflow: auto;
  display: flex;
  flex-direction: column;
}

/* 代码编辑区 textarea 自适应高度 */
.code-editor-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.code-editor-card :deep(.el-textarea) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.code-editor-card :deep(.el-textarea__inner) {
  flex: 1 !important;
  height: 100% !important;
  resize: none;
}

.workspace-hint {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  word-break: break-all;
}

.dialog-tip {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 12px;
  line-height: 1.6;
}

.dialog-tip code {
  color: var(--neon-blue);
  background: rgba(0, 242, 254, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.mode-switch {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.mode-label {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
}

.path-hint {
  display: flex;
  align-items: center;
  margin-top: 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
  border: 1px solid rgba(230, 162, 60, 0.3);
  border-radius: 6px;
  line-height: 1.5;
}

:deep(.el-tree) {
  background: transparent;
  color: var(--text-primary);
}

:deep(.el-tree-node__content:hover) {
  background: rgba(0, 242, 254, 0.1);
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--neon-blue);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px;
  background: rgba(5, 8, 17, 0.6);
  border-radius: 8px;
  border: 1px solid var(--border-glow);
  font-size: 14px;
}

:deep(.el-textarea__inner) {
  background: rgba(5, 8, 17, 0.8);
  border: 1px solid var(--border-glow);
  color: var(--text-primary);
  font-family: 'Fira Code', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--neon-blue);
  box-shadow: 0 0 20px rgba(0, 242, 254, 0.2);
}

.chat-messages {
  height: 300px;
  overflow-y: auto;
  padding: 16px;
  background: rgba(5, 8, 17, 0.6);
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid var(--border-glow);
}

.chat-msg {
  margin-bottom: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  word-wrap: break-word;
  animation: fadeIn 0.3s ease-out;
  white-space: pre-wrap;
}

.chat-msg.user {
  background: linear-gradient(135deg, rgba(240, 147, 251, 0.2), rgba(245, 87, 108, 0.2));
  border: 1px solid rgba(240, 147, 251, 0.3);
  color: white;
}

.chat-msg.assistant {
  background: rgba(16, 22, 58, 0.6);
  border: 1px solid var(--border-glow);
  color: var(--text-primary);
}

.chat-input {
  border-top: 1px solid var(--border-glow);
  padding-top: 16px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.quick-action-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.6), rgba(99, 102, 241, 0.6));
  border: 1px solid rgba(124, 58, 237, 0.4);
  transition: all 0.25s ease;
}

.quick-action-card:hover {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.85), rgba(99, 102, 241, 0.85));
  box-shadow: 0 0 16px rgba(124, 58, 237, 0.35);
  transform: translateY(-1px);
}

.quick-action-card .el-icon {
  font-size: 16px;
  color: #c084fc;
}

.cache-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: rgba(5, 8, 17, 0.6);
  border: 1px solid var(--border-glow);
  border-radius: 8px;
  font-size: 12px;
}

.cache-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cache-label {
  color: var(--text-secondary);
}

:deep(.el-textarea__inner)::-webkit-scrollbar,
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

:deep(.el-textarea__inner)::-webkit-scrollbar-track,
.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

:deep(.el-textarea__inner)::-webkit-scrollbar-thumb,
.chat-messages::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, var(--neon-blue), var(--neon-purple));
  border-radius: 3px;
}

:deep(.el-dialog) {
  background: var(--card-bg);
  border: 1px solid var(--border-glow);
  backdrop-filter: blur(10px);
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-glow);
}

:deep(.el-dialog__title) {
  color: var(--neon-blue);
  font-weight: 700;
}
</style>
