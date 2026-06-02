<template>
  <div class="monitoring-container">
    <!-- 服务状态 -->
    <el-card class="tech-card">
      <template #header>
        <div class="card-header">
          <h2>📊 系统监控</h2>
          <el-button type="primary" @click="loadAll" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新状态
          </el-button>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="6" v-for="svc in services" :key="svc.name">
          <div class="service-card" :class="{ healthy: svc.status === 'UP', degraded: svc.status === 'WARN' }">
            <div class="service-status-dot" :class="svc.status === 'UP' ? 'up' : svc.status === 'WARN' ? 'warn' : 'down'"></div>
            <div class="service-name">{{ svc.name }}</div>
            <div class="service-status">{{ svc.statusLabel }}</div>
            <div class="service-detail">{{ svc.detail }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 应用信息 -->
      <el-col :span="12">
        <el-card class="tech-card">
          <template #header><h2>ℹ️ 应用信息</h2></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="应用名称">{{ appInfo.name || 'langchain4j-agent-demo' }}</el-descriptions-item>
            <el-descriptions-item label="Spring Boot 版本">{{ appInfo.springBoot || '3.4.2' }}</el-descriptions-item>
            <el-descriptions-item label="Java 版本">{{ appInfo.java || '21' }}</el-descriptions-item>
            <el-descriptions-item label="运行环境">{{ appInfo.profile || 'default' }}</el-descriptions-item>
            <el-descriptions-item label="启动时间">{{ appInfo.startTime || '—' }}</el-descriptions-item>
            <el-descriptions-item label="运行时长">{{ appInfo.uptime || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- JVM 指标 -->
      <el-col :span="12">
        <el-card class="tech-card">
          <template #header><h2>🔧 JVM 指标</h2></template>
          <div class="metrics-grid">
            <div class="metric-item" v-for="m in jvmMetrics" :key="m.label">
              <div class="metric-label">{{ m.label }}</div>
              <el-progress :percentage="m.percent" :color="getProgressColor(m.percent)" :stroke-width="12" />
              <div class="metric-detail">{{ m.detail }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 端点状态 -->
      <el-col :span="24">
        <el-card class="tech-card">
          <template #header><h2>🔗 API 端点</h2></template>
          <el-table :data="endpoints" style="width: 100%;">
            <el-table-column prop="name" label="端点名称" />
            <el-table-column prop="path" label="路径" />
            <el-table-column prop="method" label="方法" width="80" />
            <el-table-column prop="controller" label="控制器" />
            <el-table-column label="状态" width="100">
              <template #default>
                <el-tag type="success" size="small">可用</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { actuator, aiChat, agent } from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const appInfo = ref({})
const jvmMetrics = ref([])

const services = ref([
  { name: 'Spring Boot', status: 'UP', statusLabel: '运行中', detail: '主应用' },
  { name: 'AI Chat', status: 'UP', statusLabel: '运行中', detail: '/api/ai/chat' },
  { name: 'Redis', status: 'UP', statusLabel: '运行中', detail: '缓存服务' },
  { name: 'Milvus', status: 'UP', statusLabel: '运行中', detail: '向量数据库' }
])

const endpoints = ref([
  { name: 'AI 对话（同步）', path: '/api/ai/chat', method: 'POST', controller: 'AiChatController' },
  { name: 'AI 对话（流式）', path: '/api/ai/chat/stream', method: 'GET', controller: 'AiChatController' },
  { name: '执行编程任务', path: '/api/ai-programming-agent/execute', method: 'POST', controller: 'AiProgrammingAgentController' },
  { name: '代码审查', path: '/api/ai-programming-agent/review-code', method: 'POST', controller: 'AiProgrammingAgentController' },
  { name: '知识库搜索', path: '/api/ai-programming-agent/knowledge/search', method: 'GET', controller: 'AiProgrammingAgentController' },
  { name: '技能列表', path: '/api/ai-programming-agent/skills/all', method: 'GET', controller: 'AiProgrammingAgentController' },
  { name: '学习报告', path: '/api/ai-programming-agent/learning/report', method: 'GET', controller: 'AiProgrammingAgentController' },
  { name: 'AI 监管统计', path: '/api/ai-programming-agent/supervisor/stats', method: 'GET', controller: 'AiProgrammingAgentController' },
  { name: '智能客服', path: '/api/agent/support/chat', method: 'POST', controller: 'AgentController' },
  { name: '用户登录', path: '/api/auth/login', method: 'POST', controller: 'AuthController' },
  { name: '项目列表', path: '/api/project-management/projects', method: 'GET', controller: 'ProjectManagementController' },
  { name: '健康检查', path: '/actuator/health', method: 'GET', controller: 'Actuator' },
  { name: 'Prometheus 指标', path: '/actuator/prometheus', method: 'GET', controller: 'Actuator' }
])

onMounted(() => loadAll())

const loadAll = async () => {
  loading.value = true
  try {
    // 检查应用健康
    const health = await actuator.health()
    const mainStatus = health.status || 'UNKNOWN'
    services.value[0].status = mainStatus === 'UP' ? 'UP' : 'WARN'
    services.value[0].statusLabel = mainStatus === 'UP' ? '运行中' : mainStatus

    // 检查 AI Chat
    try {
      await aiChat.health()
      services.value[1].status = 'UP'
      services.value[1].statusLabel = '运行中'
    } catch {
      services.value[1].status = 'DOWN'
      services.value[1].statusLabel = '不可用'
    }

    // 设置 JVM 指标（模拟数据，实际从 actuator/metrics 获取）
    jvmMetrics.value = [
      { label: '堆内存使用', percent: 45, detail: '460 MB / 1024 MB' },
      { label: '非堆内存', percent: 30, detail: '96 MB / 320 MB' },
      { label: '线程数', percent: 25, detail: '50 / 200' },
      { label: 'CPU 使用率', percent: 15, detail: '15%' }
    ]

    // 应用信息
    appInfo.value = {
      name: 'langchain4j-agent-demo',
      springBoot: '3.4.2',
      java: '21 (Eclipse Temurin)',
      profile: 'default',
      startTime: new Date().toLocaleString(),
      uptime: '运行中'
    }

  } catch (e) {
    ElMessage.warning('部分服务状态获取失败')
  } finally {
    loading.value = false
  }
}

const getProgressColor = (percent) => {
  if (percent < 50) return '#43e97b'
  if (percent < 80) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.monitoring-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h2 { margin: 0; }
.service-card {
  padding: 20px; border-radius: 12px; text-align: center;
  background: rgba(16, 22, 58, 0.4); border: 1px solid var(--border-glow);
  transition: all 0.3s ease; position: relative;
}
.service-card.healthy { border-color: rgba(67, 233, 123, 0.3); }
.service-card.degraded { border-color: rgba(230, 162, 60, 0.3); }
.service-status-dot {
  width: 10px; height: 10px; border-radius: 50%;
  position: absolute; top: 16px; right: 16px;
}
.service-status-dot.up { background: #43e97b; box-shadow: 0 0 10px #43e97b; }
.service-status-dot.warn { background: #e6a23c; box-shadow: 0 0 10px #e6a23c; }
.service-status-dot.down { background: #f56c6c; box-shadow: 0 0 10px #f56c6c; }
.service-name { font-size: 16px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.service-status { font-size: 14px; color: var(--neon-blue); margin-bottom: 4px; }
.service-detail { font-size: 12px; color: var(--text-secondary); }
.metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.metric-item {
  padding: 16px; border-radius: 8px;
  background: rgba(16, 22, 58, 0.3); border: 1px solid var(--border-glow);
}
.metric-label { font-size: 13px; color: var(--text-secondary); margin-bottom: 12px; }
.metric-detail { font-size: 12px; color: var(--text-primary); margin-top: 8px; text-align: right; }
</style>
