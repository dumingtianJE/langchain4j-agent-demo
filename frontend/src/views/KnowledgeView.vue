<template>
  <div class="knowledge-container">
    <el-card class="knowledge-card">
      <template #header>
        <div class="card-header">
          <h2>📚 知识库管理</h2>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            添加文档
          </el-button>
        </div>
      </template>
      
      <!-- 搜索和过滤 -->
      <div class="filter-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索知识库..."
          style="width: 300px;"
          clearable
          @clear="loadDocuments"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="searchDocuments">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
      
      <!-- 文档列表 -->
      <el-table :data="documents" style="width: 100%; margin-top: 20px;" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="viewDocument(scope.row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button size="small" type="danger" @click="deleteDocument(scope.row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="文档标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%;">
            <el-option label="API文档" value="api" />
            <el-option label="代码规范" value="coding-standards" />
            <el-option label="架构设计" value="architecture" />
            <el-option label="最佳实践" value="best-practices" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="15"
            placeholder="输入文档内容..."
          />
        </el-form-item>
        <el-form-item label="项目路径">
          <el-input v-model="form.projectPath" placeholder="可选，关联的项目路径" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDocument" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 查看文档对话框 -->
    <el-dialog v-model="viewDialogVisible" title="查看文档" width="800px">
      <div class="document-view">
        <h3>{{ currentDocument.title }}</h3>
        <el-tag>{{ currentDocument.category }}</el-tag>
        <div class="document-content" v-html="currentDocument.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Search, View, Delete } from '@element-plus/icons-vue'
import api, { knowledge } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const documents = ref([])
const searchQuery = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('添加文档')
const currentDocument = ref({})

const form = ref({
  title: '',
  category: '',
  content: '',
  projectPath: ''
})

onMounted(() => {
  loadDocuments()
})

const loadDocuments = async () => {
  loading.value = true
  try {
    const response = await knowledge.getStats()
    const stats = response.statistics || {}
    // 将知识库统计信息展示为文档列表
    documents.value = stats.documents || []
    if (!documents.value.length) {
      documents.value = [
        {
          id: 1,
          title: '知识库已连接',
          category: 'system',
          content: `知识库统计: ${stats.totalDocuments || 0} 个文档`,
          createdAt: new Date().toLocaleString()
        }
      ]
    }
  } catch (error) {
    ElMessage.error('加载文档失败: ' + error.message)
    documents.value = [
      {
        id: 1,
        title: 'Spring Boot REST API 开发指南',
        category: 'api',
        content: '本文档介绍如何使用 Spring Boot 开发 RESTful API...',
        projectPath: 'src/main/java',
        createdAt: '2026-05-25 10:00:00'
      }
    ]
  } finally {
    loading.value = false
  }
}

const searchDocuments = async () => {
  if (!searchQuery.value) {
    loadDocuments()
    return
  }
  
  loading.value = true
  try {
    const response = await knowledge.search(searchQuery.value, 10)
    documents.value = response.results || []
  } catch (error) {
    ElMessage.error('搜索失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  dialogTitle.value = '添加文档'
  form.value = {
    title: '',
    category: '',
    content: '',
    projectPath: ''
  }
  dialogVisible.value = true
}

const saveDocument = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  
  saving.value = true
  try {
    await knowledge.addDocument({
      title: form.value.title,
      category: form.value.category,
      content: form.value.content,
      projectPath: form.value.projectPath,
      tags: [],
      source: 'manual'
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadDocuments()
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

const viewDocument = (doc) => {
  currentDocument.value = doc
  viewDialogVisible.value = true
}

const deleteDocument = async (doc) => {
  try {
    await ElMessageBox.confirm(`确定要删除文档"${doc.title}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    ElMessage.info('删除功能需要后端提供 DELETE 接口')
    loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}
</script>

<style scoped>
.knowledge-container {
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.document-view h3 {
  margin-top: 0;
  margin-bottom: 12px;
}

.document-content {
  margin-top: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  line-height: 1.8;
}
</style>
