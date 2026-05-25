<template>
  <div class="chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <h2>💬 AI 编程对话</h2>
        </div>
      </template>
      
      <!-- 对话历史 -->
      <div class="messages-container" ref="messagesContainer">
        <div 
          v-for="(message, index) in messages" 
          :key="index"
          :class="['message', message.role]"
        >
          <div class="message-avatar">
            <el-icon v-if="message.role === 'user'" :size="30"><User /></el-icon>
            <el-icon v-else :size="30" color="#409eff"><Cpu /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(message.content)"></div>
            <div class="message-time">{{ message.time }}</div>
          </div>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="isLoading" class="message assistant">
          <div class="message-avatar">
            <el-icon :size="30" color="#409eff"><Cpu /></el-icon>
          </div>
          <div class="message-content">
            <el-skeleton :rows="3" animated />
          </div>
        </div>
      </div>
      
      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题，例如：帮我创建一个 Spring Boot REST API..."
          @keyup.enter.ctrl="sendMessage"
          :disabled="isLoading"
        />
        <div class="input-actions">
          <el-button @click="clearChat" :disabled="isLoading">
            <el-icon><Delete /></el-icon>
            清空对话
          </el-button>
          <el-button 
            type="primary" 
            @click="sendMessage" 
            :loading="isLoading"
            :disabled="!inputMessage.trim()"
          >
            <el-icon><Promotion /></el-icon>
            发送 (Ctrl+Enter)
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { User, Cpu, Delete, Promotion } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref(null)

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: inputMessage.value,
    time: new Date().toLocaleTimeString()
  })
  
  const userMessage = inputMessage.value
  inputMessage.value = ''
  isLoading.value = true
  
  scrollToBottom()
  
  try {
    // 调用后端 API
    const response = await api.post('/ai/chat', {
      message: userMessage
    })
    
    // 添加 AI 回复
    messages.value.push({
      role: 'assistant',
      content: response.reply || response.message || response.content,
      time: new Date().toLocaleTimeString()
    })
    
    scrollToBottom()
  } catch (error) {
    ElMessage.error('发送消息失败：' + (error.response?.data?.message || error.message))
    
    // 添加错误消息
    messages.value.push({
      role: 'assistant',
      content: '❌ 抱歉，处理您的请求时出现错误。',
      time: new Date().toLocaleTimeString(),
      isError: true
    })
  } finally {
    isLoading.value = false
  }
}

const clearChat = () => {
  messages.value = []
  ElMessage.success('对话已清空')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatMessage = (content) => {
  // 简单的代码块格式化
  return content
    .replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code style="background:#f5f5f5;padding:2px 6px;border-radius:3px;">$1</code>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.chat-container {
  padding: 30px;
  height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--card-bg);
  border: 1px solid var(--border-glow);
  backdrop-filter: blur(10px);
}

.card-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-glow);
}

.card-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--neon-blue), var(--neon-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 30px;
  background: rgba(5, 8, 17, 0.5);
}

.message {
  display: flex;
  margin-bottom: 24px;
  gap: 16px;
  animation: fadeIn 0.3s ease-out;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--neon-blue), var(--neon-purple));
  box-shadow: 0 4px 15px rgba(0, 242, 254, 0.3);
}

.message.user .message-avatar {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}

.message-content {
  max-width: 70%;
}

.message-text {
  padding: 16px 20px;
  border-radius: 16px;
  line-height: 1.6;
  word-wrap: break-word;
  font-size: 15px;
  position: relative;
}

.message.user .message-text {
  background: linear-gradient(135deg, rgba(240, 147, 251, 0.2), rgba(245, 87, 108, 0.2));
  border: 1px solid rgba(240, 147, 251, 0.3);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-text {
  background: rgba(16, 22, 58, 0.6);
  border: 1px solid var(--border-glow);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
}

.message.assistant.is-error .message-text {
  background: rgba(245, 108, 108, 0.1);
  border-color: rgba(245, 108, 108, 0.3);
  color: #f56c6c;
}

.message-time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 8px;
  padding: 0 8px;
}

.input-area {
  border-top: 1px solid var(--border-glow);
  padding: 24px;
  background: rgba(16, 22, 58, 0.4);
}

:deep(.el-textarea__inner) {
  background: rgba(5, 8, 17, 0.6);
  border: 1px solid var(--border-glow);
  color: var(--text-primary);
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--neon-blue);
  box-shadow: 0 0 20px rgba(0, 242, 254, 0.2);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 600;
}

pre {
  background: rgba(5, 8, 17, 0.8);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  border: 1px solid var(--border-glow);
  margin: 12px 0;
}

code {
  font-family: 'Fira Code', 'Courier New', monospace;
  font-size: 14px;
}

/* 滚动条 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, var(--neon-blue), var(--neon-purple));
  border-radius: 3px;
}
</style>
