<template>
  <div class="projects-container">
    <el-card class="tech-card">
      <template #header>
        <div class="card-header">
          <h2>📁 项目管理</h2>
          <el-button type="primary" @click="showRegisterDialog">
            <el-icon><Plus /></el-icon>
            注册项目
          </el-button>
        </div>
      </template>

      <!-- 项目列表 -->
      <el-row :gutter="20" v-loading="loading">
        <el-col :span="8" v-for="project in projects" :key="project.projectId">
          <el-card class="project-card" shadow="hover" @click="selectProject(project)">
            <div class="project-header">
              <div class="project-icon" :style="{ background: getRandomGradient(project.projectId) }">
                {{ (project.projectName || project.projectId || 'P').charAt(0).toUpperCase() }}
              </div>
              <div class="project-info">
                <h3>{{ project.projectName || project.projectId }}</h3>
                <el-tag size="small">{{ project.techStack || '通用' }}</el-tag>
              </div>
            </div>
            <p class="project-desc">{{ project.domain || project.description || '暂无描述' }}</p>
            <div class="project-stats">
              <el-tag size="small" type="info">知识: {{ project.knowledgeCount || 0 }}</el-tag>
              <el-tag size="small" type="success">示例: {{ project.exampleCount || 0 }}</el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!projects.length && !loading" description="暂无注册项目，点击右上角注册" />
    </el-card>

    <!-- 项目详情面板 -->
    <el-drawer v-model="drawerVisible" :title="selectedProject?.projectName || '项目详情'" size="60%" direction="rtl">
      <div class="drawer-content" v-if="selectedProject">
        <el-tabs v-model="activeTab">
          <!-- 代码生成 -->
          <el-tab-pane label="代码生成" name="generate">
            <el-form label-position="top">
              <el-form-item label="需求描述">
                <el-input v-model="codeReq" type="textarea" :rows="4" placeholder="描述你需要生成的代码功能..." />
              </el-form-item>
              <el-button type="primary" @click="generateCode" :loading="generating" style="width: 100%;">
                生成代码
              </el-button>
            </el-form>
            <div v-if="generatedCode" class="code-output">
              <h4>生成结果</h4>
              <pre>{{ generatedCode }}</pre>
            </div>
          </el-tab-pane>

          <!-- 代码优化 -->
          <el-tab-pane label="代码优化" name="optimize">
            <el-form label-position="top">
              <el-form-item label="现有代码">
                <el-input v-model="optCode" type="textarea" :rows="6" placeholder="粘贴需要优化的代码..." />
              </el-form-item>
              <el-form-item label="优化目标">
                <el-input v-model="optGoal" placeholder="例如：提升性能、改善可读性" />
              </el-form-item>
              <el-button type="primary" @click="optimizeCode" :loading="optimizing" style="width: 100%;">
                优化代码
              </el-button>
            </el-form>
            <div v-if="optimizeResult" class="code-output">
              <h4>优化结果（提升 {{ optimizeResult.improvement || '0%' }}）</h4>
              <pre>{{ optimizeResult.optimizedCode }}</pre>
            </div>
          </el-tab-pane>

          <!-- 代码示例 -->
          <el-tab-pane label="代码示例" name="examples">
            <el-button @click="loadExamples" :loading="examplesLoading" style="margin-bottom: 16px;">
              加载示例
            </el-button>
            <div v-for="(ex, i) in examples" :key="i" class="example-item">
              <h4>{{ ex.title }}</h4>
              <el-tag v-for="tag in (ex.tags || [])" :key="tag" size="small" style="margin-right: 4px;">{{ tag }}</el-tag>
              <pre>{{ ex.code }}</pre>
            </div>
            <el-empty v-if="!examples.length && !examplesLoading" description="暂无代码示例" />
          </el-tab-pane>

          <!-- 项目搜索 -->
          <el-tab-pane label="知识搜索" name="search">
            <div class="search-bar">
              <el-input v-model="searchQuery" placeholder="搜索项目知识库..." @keyup.enter="searchProject" />
              <el-button type="primary" @click="searchProject">搜索</el-button>
            </div>
            <div v-for="(r, i) in searchResults" :key="i" class="search-result">
              <p>{{ r.content || r.text || JSON.stringify(r) }}</p>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <!-- 注册项目对话框 -->
    <el-dialog v-model="registerDialogVisible" title="注册新项目" width="600px">
      <el-form :model="registerForm" label-width="100px">
        <el-form-item label="项目 ID">
          <el-input v-model="registerForm.projectId" placeholder="唯一标识，如 my-project" />
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="registerForm.projectName" placeholder="项目显示名称" />
        </el-form-item>
        <el-form-item label="项目路径">
          <el-input v-model="registerForm.projectPath" placeholder="可选，如 /workspace/my-project" />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="registerForm.techStack" placeholder="如 Java, Spring Boot" />
        </el-form-item>
        <el-form-item label="领域">
          <el-input v-model="registerForm.domain" placeholder="如 电商、金融" />
        </el-form-item>
        <el-form-item label="全局共享">
          <el-switch v-model="registerForm.enableGlobalSharing" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="registerProject" :loading="registering">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { projectManagement } from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const projects = ref([])
const selectedProject = ref(null)
const drawerVisible = ref(false)
const activeTab = ref('generate')

const codeReq = ref('')
const generatedCode = ref('')
const generating = ref(false)
const optCode = ref('')
const optGoal = ref('')
const optimizeResult = ref(null)
const optimizing = ref(false)
const examples = ref([])
const examplesLoading = ref(false)
const searchQuery = ref('')
const searchResults = ref([])

const registerDialogVisible = ref(false)
const registering = ref(false)
const registerForm = ref({
  projectId: '', projectName: '', projectPath: '', techStack: '', domain: '', enableGlobalSharing: false
})

const gradients = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)'
]
const getRandomGradient = (id) => gradients[(id || '').length % gradients.length]

onMounted(() => loadProjects())

const loadProjects = async () => {
  loading.value = true
  try {
    const res = await projectManagement.getAllProjects()
    projects.value = res.projects || []
  } catch (e) {
    ElMessage.error('加载项目列表失败')
    projects.value = []
  } finally {
    loading.value = false
  }
}

const selectProject = (project) => {
  selectedProject.value = project
  drawerVisible.value = true
  activeTab.value = 'generate'
  generatedCode.value = ''
  optimizeResult.value = null
  examples.value = []
  searchResults.value = []
}

const generateCode = async () => {
  if (!codeReq.value.trim()) return ElMessage.warning('请输入需求描述')
  generating.value = true
  try {
    const res = await projectManagement.generateCode(selectedProject.value.projectId, codeReq.value)
    generatedCode.value = res.code || '生成完成'
    ElMessage.success('代码生成完成')
  } catch (e) {
    ElMessage.error('生成失败: ' + e.message)
  } finally {
    generating.value = false
  }
}

const optimizeCode = async () => {
  if (!optCode.value.trim()) return ElMessage.warning('请输入代码')
  optimizing.value = true
  try {
    const res = await projectManagement.optimizeCode(selectedProject.value.projectId, optCode.value, optGoal.value)
    optimizeResult.value = res
    ElMessage.success('优化完成')
  } catch (e) {
    ElMessage.error('优化失败')
  } finally {
    optimizing.value = false
  }
}

const loadExamples = async () => {
  examplesLoading.value = true
  try {
    const res = await projectManagement.getCodeExamples(selectedProject.value.projectId)
    examples.value = res.examples || []
  } catch (e) {
    ElMessage.error('加载示例失败')
  } finally {
    examplesLoading.value = false
  }
}

const searchProject = async () => {
  if (!searchQuery.value.trim()) return
  try {
    const res = await projectManagement.searchKnowledge(selectedProject.value.projectId, searchQuery.value)
    searchResults.value = res.results || []
  } catch (e) {
    ElMessage.error('搜索失败')
  }
}

const showRegisterDialog = () => {
  registerForm.value = { projectId: '', projectName: '', projectPath: '', techStack: '', domain: '', enableGlobalSharing: false }
  registerDialogVisible.value = true
}

const registerProject = async () => {
  if (!registerForm.value.projectId) return ElMessage.warning('请输入项目 ID')
  registering.value = true
  try {
    await projectManagement.register(registerForm.value)
    ElMessage.success('项目注册成功')
    registerDialogVisible.value = false
    loadProjects()
  } catch (e) {
    ElMessage.error('注册失败: ' + e.message)
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.projects-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h2 { margin: 0; }
.project-card { margin-bottom: 20px; cursor: pointer; transition: all 0.3s ease; }
.project-card:hover { transform: translateY(-6px); box-shadow: 0 12px 40px rgba(0, 242, 254, 0.2); }
.project-header { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; }
.project-icon {
  width: 48px; height: 48px; border-radius: 12px; display: flex;
  align-items: center; justify-content: center; color: white; font-size: 20px; font-weight: 700;
}
.project-info h3 { margin: 0 0 6px; font-size: 16px; }
.project-desc { color: var(--text-secondary); font-size: 13px; line-height: 1.5; margin-bottom: 12px; min-height: 40px; }
.project-stats { display: flex; gap: 8px; }
.drawer-content { padding: 0 8px; }
.code-output { margin-top: 20px; }
.code-output h4 { color: var(--neon-blue); margin-bottom: 8px; }
.code-output pre {
  background: rgba(5, 8, 17, 0.8); padding: 16px; border-radius: 8px;
  border: 1px solid var(--border-glow); color: var(--text-primary);
  font-family: 'Fira Code', monospace; font-size: 13px; line-height: 1.6;
  overflow-x: auto; white-space: pre-wrap; max-height: 400px; overflow-y: auto;
}
.example-item { margin-bottom: 16px; padding: 16px; background: rgba(16, 22, 58, 0.3); border-radius: 8px; }
.example-item h4 { margin: 0 0 8px; }
.example-item pre {
  background: rgba(5, 8, 17, 0.6); padding: 12px; border-radius: 6px; margin-top: 8px;
  border: 1px solid var(--border-glow); font-size: 12px; white-space: pre-wrap;
}
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.search-bar .el-input { flex: 1; }
.search-result {
  padding: 12px 16px; margin-bottom: 8px; border-radius: 8px;
  background: rgba(16, 22, 58, 0.3); border-left: 3px solid var(--neon-blue);
}
</style>
