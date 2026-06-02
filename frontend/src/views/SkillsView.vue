<template>
  <div class="skills-container">
    <el-card class="skills-card">
      <template #header>
        <div class="card-header">
          <h2>⭐ 技能管理</h2>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            添加技能
          </el-button>
        </div>
      </template>
      
      <!-- 技能列表 -->
      <el-row :gutter="20" v-loading="loading">
        <el-col :span="8" v-for="skill in skillsList" :key="skill.id">
          <el-card class="skill-card" shadow="hover">
            <div class="skill-header">
              <el-icon :size="40" :color="getSkillColor(skill.level)"><Trophy /></el-icon>
              <div class="skill-info">
                <h3>{{ skill.name }}</h3>
                <el-rate v-model="skill.level" disabled :max="5" />
              </div>
            </div>
            
            <div class="skill-description">
              {{ skill.description }}
            </div>
            
            <div class="skill-stats">
              <el-tag size="small" type="info">
                使用次数: {{ skill.usageCount || 0 }}
              </el-tag>
              <el-tag size="small" type="success">
                成功率: {{ skill.successRate || 0 }}%
              </el-tag>
            </div>
            
            <div class="skill-actions">
              <el-button size="small" @click="editSkill(skill)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button size="small" type="danger" @click="deleteSkill(skill)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 空状态 -->
      <el-empty v-if="skillsList.length === 0 && !loading" description="暂无技能" />
    </el-card>
    
    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="技能名称">
          <el-input v-model="form.name" placeholder="例如: Java Spring Boot" />
        </el-form-item>
        <el-form-item label="技能等级">
          <el-rate v-model="form.level" :max="5" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="技能描述..."
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="form.keywords"
            placeholder="多个关键词用逗号分隔，例如: java,spring,boot"
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSkill" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Trophy, Edit, Delete } from '@element-plus/icons-vue'
import api, { skills } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const skillsList = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('添加技能')
const editingSkill = ref(null)

const form = ref({
  name: '',
  level: 3,
  description: '',
  keywords: '',
  enabled: true
})

onMounted(() => {
  loadSkills()
})

const loadSkills = async () => {
  loading.value = true
  try {
    const response = await skills.getAll()
    skillsList.value = response.skills || []
  } catch (error) {
    ElMessage.error('加载技能失败: ' + error.message)
    skillsList.value = [
      {
        id: 1,
        name: 'Java Spring Boot',
        level: 5,
        description: '精通 Spring Boot 框架开发，包括 REST API、数据访问、安全认证等',
        keywords: 'java,spring,boot,rest',
        enabled: true,
        usageCount: 156,
        successRate: 95
      },
      {
        id: 2,
        name: 'Vue.js 前端开发',
        level: 4,
        description: '熟练使用 Vue.js 开发前端应用，包括组件化、状态管理、路由等',
        keywords: 'vue,javascript,frontend',
        enabled: true,
        usageCount: 89,
        successRate: 88
      },
      {
        id: 3,
        name: '数据库设计',
        level: 4,
        description: '数据库建模、SQL 优化、索引设计等',
        keywords: 'mysql,postgresql,database',
        enabled: true,
        usageCount: 67,
        successRate: 92
      }
    ]
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  dialogTitle.value = '添加技能'
  editingSkill.value = null
  form.value = {
    name: '',
    level: 3,
    description: '',
    keywords: '',
    enabled: true
  }
  dialogVisible.value = true
}

const editSkill = (skill) => {
  dialogTitle.value = '编辑技能'
  editingSkill.value = skill
  form.value = {
    name: skill.name,
    level: skill.level,
    description: skill.description,
    keywords: skill.keywords || '',
    enabled: skill.enabled
  }
  dialogVisible.value = true
}

const saveSkill = async () => {
  if (!form.value.name) {
    ElMessage.warning('请填写技能名称')
    return
  }
  
  saving.value = true
  try {
    await skills.add({
      name: form.value.name,
      level: form.value.level,
      description: form.value.description,
      keywords: form.value.keywords ? form.value.keywords.split(',').map(k => k.trim()) : [],
      enabled: form.value.enabled
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadSkills()
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

const deleteSkill = async (skill) => {
  try {
    await ElMessageBox.confirm(`确定要删除技能"${skill.name}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    ElMessage.info('删除功能需要后端提供 DELETE 接口')
    loadSkills()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const getSkillColor = (level) => {
  if (level >= 5) return '#f56c6c'
  if (level >= 4) return '#e6a23c'
  if (level >= 3) return '#409eff'
  return '#909399'
}
</script>

<style scoped>
.skills-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
}

.skill-card {
  margin-bottom: 20px;
}

.skill-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.skill-info h3 {
  margin: 0 0 8px 0;
  color: #303133;
}

.skill-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
  min-height: 60px;
}

.skill-stats {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.skill-actions {
  display: flex;
  gap: 8px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
</style>
