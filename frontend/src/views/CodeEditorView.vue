<template>
  <div class="editor-container">
    <el-card class="editor-card">
      <template #header>
        <div class="card-header">
          <h2>📝 代码编辑器</h2>
          <div class="header-actions">
            <el-button @click="loadFile">
              <el-icon><FolderOpened /></el-icon>
              加载文件
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
          <el-card class="file-tree-card">
            <template #header>
              <div class="tree-header">
                <span>文件目录</span>
                <el-button size="small" @click="refreshFiles">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </template>
            <el-tree
              :data="fileTree"
              :props="treeProps"
              @node-click="handleFileClick"
              highlight-current
            />
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
            <div class="chat-messages" ref="chatMessages">
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
    
    <!-- 加载文件对话框 -->
    <el-dialog v-model="loadDialogVisible" title="加载文件">
      <el-input v-model="loadFilePath" placeholder="输入文件路径，例如: src/main/java/..." />
      <template #footer>
        <el-button @click="loadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmLoadFile">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { FolderOpened, Download, MagicStick, Refresh } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const codeContent = ref('')
const currentFile = ref('')
const isModified = ref(false)
const isSaving = ref(false)
const loadDialogVisible = ref(false)
const loadFilePath = ref('')
const aiInput = ref('')
const aiLoading = ref(false)
const chatMessages = ref([])

const fileTree = ref([
  {
    label: 'src',
    children: [
      {
        label: 'main',
        children: [
          { label: 'java', children: [] },
          { label: 'resources', children: [] }
        ]
      },
      {
        label: 'test',
        children: []
      }
    ]
  }
])

const treeProps = {
  children: 'children',
  label: 'label'
}

const markModified = () => {
  isModified.value = true
}

const loadFile = () => {
  loadDialogVisible.value = true
  loadFilePath.value = ''
}

const confirmLoadFile = async () => {
  if (!loadFilePath.value) {
    ElMessage.warning('请输入文件路径')
    return
  }
  
  try {
    const response = await api.get(`/code/read?path=${encodeURIComponent(loadFilePath.value)}`)
    codeContent.value = response.content || response.code || ''
    currentFile.value = loadFilePath.value
    isModified.value = false
    loadDialogVisible.value = false
    ElMessage.success('文件加载成功')
  } catch (error) {
    ElMessage.error('加载文件失败: ' + (error.response?.data?.message || error.message))
  }
}

const saveFile = async () => {
  if (!currentFile.value) {
    ElMessage.warning('请先加载文件')
    return
  }
  
  isSaving.value = true
  
  try {
    await api.post('/code/write', {
      path: currentFile.value,
      content: codeContent.value,
      description: '通过前端编辑器保存'
    })
    
    isModified.value = false
    ElMessage.success('文件保存成功')
  } catch (error) {
    ElMessage.error('保存文件失败: ' + (error.response?.data?.message || error.message))
  } finally {
    isSaving.value = false
  }
}

const refreshFiles = () => {
  ElMessage.info('刷新文件树（演示功能）')
}

const handleFileClick = (data) => {
  if (!data.children) {
    loadFilePath.value = data.label
    confirmLoadFile()
  }
}

const askAI = () => {
  ElMessage.info('请在右侧 AI 助手面板中输入问题')
}

const sendToAI = async () => {
  if (!aiInput.value.trim() || aiLoading.value) return
  
  const userMessage = aiInput.value
  chatMessages.value.push({
    role: 'user',
    content: userMessage
  })
  
  aiInput.value = ''
  aiLoading.value = true
  
  try {
    const response = await api.post('/ai/chat', {
      message: userMessage,
      codeContext: codeContent.value.substring(0, 1000) // 发送部分代码上下文
    })
    
    chatMessages.value.push({
      role: 'assistant',
      content: response.reply || response.message || '收到回复'
    })
    
    scrollToBottom()
  } catch (error) {
    ElMessage.error('AI 请求失败')
    chatMessages.value.push({
      role: 'assistant',
      content: '❌ 请求失败: ' + error.message
    })
  } finally {
    aiLoading.value = false
  }
}

const scrollToBottom = () => {
  setTimeout(() => {
    const container = document.querySelector('.chat-messages')
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

/* 滚动条 */
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

/* 对话框样式 */
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
