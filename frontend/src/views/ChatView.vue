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
  padding: 20px;
  height: calc(100vh - 40px);
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header h2 {
  margin: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
  border-radius: 4px;
  margin-bottom: 20px;
}

.message {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
  word-wrap: break-word;
}

.message.user .message-text {
  background: #409eff;
  color: white;
}

.message.assistant .message-text {
  background: white;
  border: 1px solid #e4e7ed;
  color: #303133;
}

.message.assistant.is-error .message-text {
  background: #fef0f0;
  border-color: #fbc4c4;
  color: #f56c6c;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.input-area {
  border-top: 1px solid #e4e7ed;
  padding-top: 20px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
}

pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
}

code {
  font-family: 'Courier New', monospace;
}
</style>
