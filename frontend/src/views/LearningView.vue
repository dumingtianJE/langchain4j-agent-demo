<template>
  <div class="learning-container">
    <el-row :gutter="20">
      <!-- 左侧：学习报告 -->
      <el-col :span="14">
        <el-card class="tech-card">
          <template #header>
            <div class="card-header">
              <h2>🧠 学习中心</h2>
              <el-button type="primary" @click="loadReport" :loading="loading">
                <el-icon><Refresh /></el-icon>
                刷新报告
              </el-button>
            </div>
          </template>

          <div v-loading="loading">
            <!-- 学习统计 -->
            <el-row :gutter="16" class="stats-row">
              <el-col :span="6" v-for="stat in statCards" :key="stat.label">
                <div class="stat-card">
                  <div class="stat-value">{{ stat.value }}</div>
                  <div class="stat-label">{{ stat.label }}</div>
                </div>
              </el-col>
            </el-row>

            <!-- 学习报告 -->
            <div class="report-section" v-if="report">
              <h3>📊 学习报告</h3>
              <pre class="report-content">{{ typeof report === 'string' ? report : JSON.stringify(report, null, 2) }}</pre>
            </div>

            <!-- 学习经验列表 -->
            <div class="experiences-section" v-if="experiences.length">
              <h3>📝 学习经验</h3>
              <el-timeline>
                <el-timeline-item
                  v-for="(exp, index) in experiences"
                  :key="index"
                  :timestamp="exp.createdAt || exp.timestamp"
                  placement="top"
                >
                  <div class="exp-item">
                    <el-tag size="small" :type="getExpType(exp.feedbackScore)">{{ exp.category || 'general' }}</el-tag>
                    <p>{{ exp.userFeedback || exp.query || exp.response }}</p>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：手动学习 -->
      <el-col :span="10">
        <el-card class="tech-card">
          <template #header>
            <h2>✏️ 手动学习</h2>
          </template>

          <el-form :model="learnForm" label-position="top">
            <el-form-item label="知识分类">
              <el-select v-model="learnForm.category" style="width: 100%;" placeholder="选择分类">
                <el-option label="编程技巧" value="programming" />
                <el-option label="架构设计" value="architecture" />
                <el-option label="最佳实践" value="best-practices" />
                <el-option label="性能优化" value="performance" />
                <el-option label="安全规范" value="security" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="知识内容">
              <el-input
                v-model="learnForm.knowledge"
                type="textarea"
                :rows="8"
                placeholder="输入希望 AI 学习的新知识，例如：&#10;在 Spring Boot 中使用 @Async 注解时，必须确保方法不在同一个类中调用..."
              />
            </el-form-item>
            <el-button type="primary" @click="submitLearning" :loading="saving" style="width: 100%;">
              <el-icon><Promotion /></el-icon>
              提交学习
            </el-button>
          </el-form>
        </el-card>

        <!-- 反馈表单 -->
        <el-card class="tech-card" style="margin-top: 20px;">
          <template #header>
            <h2>💬 经验反馈</h2>
          </template>

          <el-form :model="feedbackForm" label-position="top">
            <el-form-item label="反馈类型">
              <el-radio-group v-model="feedbackForm.type">
                <el-radio value="positive">👍 正面</el-radio>
                <el-radio value="negative">👎 改进</el-radio>
                <el-radio value="suggestion">💡 建议</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="反馈内容">
              <el-input v-model="feedbackForm.content" type="textarea" :rows="4" placeholder="描述您的反馈..." />
            </el-form-item>
            <el-button type="success" @click="submitFeedback" :loading="feedbackSaving" style="width: 100%;">
              提交反馈
            </el-button>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Promotion } from '@element-plus/icons-vue'
import { learning } from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const feedbackSaving = ref(false)
const report = ref(null)
const experiences = ref([])
const statistics = ref({})

const learnForm = ref({ knowledge: '', category: 'programming' })
const feedbackForm = ref({ type: 'positive', content: '' })

const statCards = computed(() => [
  { label: '学习次数', value: statistics.value.totalExperiences || 0 },
  { label: '正面经验', value: statistics.value.positiveExperiences || 0 },
  { label: '改进项', value: statistics.value.negativeExperiences || 0 },
  { label: '改进模式', value: statistics.value.improvementPatterns || 0 }
])

onMounted(() => loadReport())

const loadReport = async () => {
  loading.value = true
  try {
    const response = await learning.getReport()
    report.value = response.report
    statistics.value = response.statistics || {}
    experiences.value = response.report?.recentExperiences || response.report?.experiences || []
  } catch (error) {
    ElMessage.error('加载学习报告失败')
  } finally {
    loading.value = false
  }
}

const submitLearning = async () => {
  if (!learnForm.value.knowledge.trim()) {
    ElMessage.warning('请输入知识内容')
    return
  }
  saving.value = true
  try {
    await learning.learnKnowledge(learnForm.value.knowledge, learnForm.value.category)
    ElMessage.success('知识已提交学习')
    learnForm.value.knowledge = ''
    loadReport()
  } catch (error) {
    ElMessage.error('提交失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

const submitFeedback = async () => {
  if (!feedbackForm.value.content.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }
  feedbackSaving.value = true
  try {
    await learning.recordFeedback({
      userFeedback: feedbackForm.value.content,
      category: feedbackForm.value.type,
      feedbackScore: feedbackForm.value.type === 'positive' ? 5 : feedbackForm.value.type === 'negative' ? 2 : 3
    })
    ElMessage.success('反馈已提交')
    feedbackForm.value.content = ''
    loadReport()
  } catch (error) {
    ElMessage.error('提交反馈失败')
  } finally {
    feedbackSaving.value = false
  }
}

const getExpType = (rating) => {
  if (rating >= 4) return 'success'
  if (rating >= 3) return 'warning'
  return 'danger'
}
</script>

<style scoped>
.learning-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h2, .tech-card :deep(.el-card__header) h2 { margin: 0; }
.stats-row { margin-bottom: 24px; }
.stat-card {
  text-align: center; padding: 20px; border-radius: 12px;
  background: linear-gradient(135deg, rgba(0, 242, 254, 0.1), rgba(178, 36, 239, 0.1));
  border: 1px solid var(--border-glow);
}
.stat-value {
  font-size: 32px; font-weight: 700;
  background: linear-gradient(135deg, var(--neon-blue), var(--neon-purple));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 8px; }
.report-section, .experiences-section { margin-top: 24px; }
.report-section h3, .experiences-section h3 { color: var(--neon-blue); margin-bottom: 16px; }
.report-content {
  background: rgba(5, 8, 17, 0.6); padding: 16px; border-radius: 8px;
  border: 1px solid var(--border-glow); color: var(--text-primary);
  font-family: 'Fira Code', monospace; font-size: 13px; line-height: 1.8;
  white-space: pre-wrap; max-height: 400px; overflow-y: auto;
}
.exp-item { padding: 4px 0; }
.exp-item p { margin: 8px 0 0; color: var(--text-primary); line-height: 1.6; }
</style>
