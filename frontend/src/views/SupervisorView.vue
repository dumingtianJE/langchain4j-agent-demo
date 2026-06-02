<template>
  <div class="supervisor-container">
    <el-row :gutter="20">
      <!-- Token 统计 -->
      <el-col :span="24">
        <el-card class="tech-card">
          <template #header>
            <div class="card-header">
              <h2>🔍 AI 监管面板</h2>
              <el-button type="primary" @click="loadAll" :loading="loading">
                <el-icon><Refresh /></el-icon>
                刷新数据
              </el-button>
            </div>
          </template>

          <el-row :gutter="16" class="stats-row">
            <el-col :span="4" v-for="stat in statCards" :key="stat.label">
              <div class="stat-card" :style="{ borderColor: stat.color }">
                <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
                <div class="stat-label">{{ stat.label }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 监管报告 -->
      <el-col :span="14">
        <el-card class="tech-card" style="height: 500px;">
          <template #header>
            <h2>📋 监管报告</h2>
          </template>
          <div v-loading="reportLoading" class="report-content">
            <pre v-if="report">{{ report }}</pre>
            <el-empty v-else description="暂无报告数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 警报日志 -->
      <el-col :span="10">
        <el-card class="tech-card" style="height: 500px;">
          <template #header>
            <div class="card-header">
              <h2>🚨 警报日志</h2>
              <el-select v-model="alertLimit" size="small" style="width: 100px;" @change="loadAlerts">
                <el-option label="最近 10" :value="10" />
                <el-option label="最近 20" :value="20" />
                <el-option label="最近 50" :value="50" />
              </el-select>
            </div>
          </template>
          <div v-loading="alertsLoading" class="alerts-list">
            <div v-for="(alert, index) in alerts" :key="index" class="alert-item" :class="getAlertLevel(alert)">
              <div class="alert-time">{{ alert.timestamp || alert.time || '—' }}</div>
              <div class="alert-msg">{{ alert.message || alert.content || JSON.stringify(alert) }}</div>
            </div>
            <el-empty v-if="!alerts.length && !alertsLoading" description="暂无警报" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { supervisor } from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const reportLoading = ref(false)
const alertsLoading = ref(false)
const stats = ref({})
const report = ref('')
const alerts = ref([])
const alertLimit = ref(20)

const statCards = computed(() => [
  { label: '总请求数', value: stats.value.totalRecords || 0, color: '#00f2fe' },
  { label: '总 Token', value: stats.value.totalTokensUsed || 0, color: '#b224ef' },
  { label: '平均 Token/请求', value: stats.value.averageTokensPerRequest || 0, color: '#43e97b' },
  { label: '并发请求', value: stats.value.currentConcurrentRequests || 0, color: '#f5576c' },
  { label: '今日 Token', value: stats.value.currentDayUsage || 0, color: '#e6a23c' },
  { label: '警报数量', value: stats.value.alertCount || 0, color: '#667eea' }
])

onMounted(() => loadAll())

const loadAll = () => {
  loadStats()
  loadReport()
  loadAlerts()
}

const loadStats = async () => {
  loading.value = true
  try {
    const res = await supervisor.getStats()
    stats.value = res.statistics || res.stats || {}
  } catch (e) {
    ElMessage.error('加载统计失败')
  } finally {
    loading.value = false
  }
}

const loadReport = async () => {
  reportLoading.value = true
  try {
    const res = await supervisor.getReport()
    report.value = res.report || ''
  } catch (e) {
    report.value = '暂无报告'
  } finally {
    reportLoading.value = false
  }
}

const loadAlerts = async () => {
  alertsLoading.value = true
  try {
    const res = await supervisor.getAlerts(alertLimit.value)
    alerts.value = res.alerts || []
  } catch (e) {
    alerts.value = []
  } finally {
    alertsLoading.value = false
  }
}

const getAlertLevel = (alert) => {
  const level = (alert.level || alert.severity || '').toLowerCase()
  if (level.includes('critical') || level.includes('error')) return 'critical'
  if (level.includes('warn')) return 'warning'
  return 'info'
}
</script>

<style scoped>
.supervisor-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h2 { margin: 0; }
.stats-row { margin-bottom: 8px; }
.stat-card {
  text-align: center; padding: 20px 12px; border-radius: 12px;
  background: rgba(16, 22, 58, 0.4); border: 1px solid var(--border-glow);
  transition: all 0.3s ease;
}
.stat-card:hover { transform: translateY(-4px); box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3); }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 12px; color: var(--text-secondary); margin-top: 8px; }
.report-content {
  height: 400px; overflow-y: auto;
  background: rgba(5, 8, 17, 0.6); padding: 16px; border-radius: 8px;
  border: 1px solid var(--border-glow);
}
.report-content pre {
  margin: 0; color: var(--text-primary); font-family: 'Fira Code', monospace;
  font-size: 13px; line-height: 1.8; white-space: pre-wrap;
}
.alerts-list { height: 400px; overflow-y: auto; }
.alert-item {
  padding: 12px 16px; margin-bottom: 8px; border-radius: 8px;
  border-left: 3px solid var(--border-glow); background: rgba(16, 22, 58, 0.3);
  transition: all 0.2s ease;
}
.alert-item:hover { background: rgba(16, 22, 58, 0.6); }
.alert-item.critical { border-left-color: #f56c6c; background: rgba(245, 108, 108, 0.05); }
.alert-item.warning { border-left-color: #e6a23c; background: rgba(230, 162, 60, 0.05); }
.alert-item.info { border-left-color: #409eff; }
.alert-time { font-size: 11px; color: var(--text-secondary); margin-bottom: 4px; }
.alert-msg { font-size: 13px; color: var(--text-primary); line-height: 1.5; }
</style>
