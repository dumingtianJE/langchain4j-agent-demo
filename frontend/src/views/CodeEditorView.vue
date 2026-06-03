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
      
      <el-row :gutter="20" style="height: calc(100vh - 200px);">
        <!-- 文件树 -->
        <el-col :span="5">
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
        </el-col>
        
        <!-- 代码编辑区 -->
        <el-col :span="14">
          <el-card class="code-editor-card">
            <div class="editor-info">
              <span>当前文件: {{ currentFile || '未选择' }}</span>
              <el-tag v-if="isModified" type="warning">已修改</el-tag>
            </div>
            <el-input
              v-model="codeContent"
              type="textarea"
              :rows="30"
              placeholder="在此编辑代码..."
              class="code-textarea"
              @input="markModified"
            />
          </el-card>
        </el-col>
        
        <!-- AI 对话区 -->
        <el-col :span="5">
          <el-card class="ai-chat-card">
            <template #header>
              <span>AI 助手</span>
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
        </el-col>
      </el-row>
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
import { ref, onMounted } from 'vue'
import { FolderOpened, Download, MagicStick, Refresh, WarningFilled } from '@element-plus/icons-vue'
import { codeFile, aiChat } from '../api'
import { ElMessage } from 'element-plus'

const codeContent = ref('')
const currentFile = ref('')
const isModified = ref(false)
const isSaving = ref(false)
const loadDialogVisible = ref(false)
const loadFilePath = ref('.')
const loadMode = ref('local')
const isDockerEnv = ref(false)   // 后端是否运行在 Docker/Linux 容器中
const pathHint = ref('')          // 路径输入提示信息
const aiInput = ref('')
const aiLoading = ref(false)
const chatMessages = ref([])
const chatMessagesRef = ref(null)

const fileTree = ref([])
const projectRoot = ref('')
const workspaceRoot = ref('')
const treeLoading = ref(false)

const treeProps = {
  children: 'children',
  label: 'name'
}

onMounted(async () => {
  try {
    const res = await codeFile.getWorkspace()
    workspaceRoot.value = res.workspaceRoot || ''
    // 根据工作区路径判断后端是否运行在 Docker/Linux 容器中
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
})

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
    ElMessage.success(`项目已加载: ${data.absolutePath || path}`)
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
  
  const userMessage = aiInput.value
  chatMessages.value.push({ role: 'user', content: userMessage })
  aiInput.value = ''
  aiLoading.value = true
  
  try {
    // 仅在有代码内容时才传 codeContext
    const context = codeContent.value?.trim() ? codeContent.value.substring(0, 8000) : null
    const response = await aiChat.chat(userMessage, context)
    chatMessages.value.push({
      role: 'assistant',
      content: response.reply || response.message || response.result || '收到回复'
    })
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
    scrollToBottom()
  } finally {
    aiLoading.value = false
  }
}

/** 快捷操作：解析项目业务逻辑 / 架构 */
const quickAction = (type) => {
  if (aiLoading.value) {
    ElMessage.warning('AI 正在处理中，请稍候...')
    return
  }
  if (type === 'logic') {
    aiInput.value = '请详细分析并解析当前项目的业务逻辑，包括核心业务流程、主要功能模块、数据流向以及各模块之间的协作关系。'
  } else if (type === 'arch') {
    aiInput.value = '请详细分析并解析当前项目的业务架构，包括系统分层设计、技术选型、模块划分、服务间通信方式、以及整体架构的优缺点。'
  }
  sendToAI()
}

const scrollToBottom = () => {
  setTimeout(() => {
    const container = chatMessagesRef.value || document.querySelector('.chat-messages')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  }, 100)
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
