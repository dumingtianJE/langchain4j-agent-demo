<template>
  <div class="knowledge-container">
    <el-card class="knowledge-card">
      <template #header>
        <div class="card-header">
          <h2>📚 知识库管理</h2>
          <div class="header-actions">
            <el-tag type="info" size="large">共 {{ documents.length }} 篇文档</el-tag>
            <el-button type="primary" @click="showAddDialog">
              <el-icon><Plus /></el-icon>
              添加文档
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 搜索和过滤 -->
      <div class="filter-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索知识库文档..."
          style="width: 300px;"
          clearable
          @clear="loadDocuments"
          @keyup.enter="searchDocuments"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="searchDocuments">
          <el-icon><Search /></el-icon>
          语义搜索
        </el-button>
        <el-button @click="loadDocuments">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
      
      <!-- 文档列表 -->
      <el-table :data="documents" style="width: 100%; margin-top: 20px;" v-loading="loading"
                :row-class-name="tableRowClassName">
        <el-table-column prop="id" label="ID" width="100" show-overflow-tooltip>
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.id?.substring(0, 8) || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="scope">
            <el-tag :type="getCategoryTagType(scope.row.category)" size="small">
              {{ scope.row.category || '未分类' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" width="200" show-overflow-tooltip>
          <template #default="scope">
            <el-tag
              v-for="tag in (scope.row.tags || []).slice(0, 3)"
              :key="tag"
              size="small"
              style="margin-right: 4px; margin-bottom: 2px;"
            >{{ tag }}</el-tag>
            <el-tag v-if="(scope.row.tags || []).length > 3" size="small" type="info">
              +{{ scope.row.tags.length - 3 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accessCount" label="访问" width="80" align="center">
          <template #default="scope">
            {{ scope.row.accessCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="viewDocument(scope.row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button size="small" type="success" @click="downloadDocument(scope.row)">
              <el-icon><Download /></el-icon>
              下载
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
            <el-option label="技术文档" value="技术文档" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="15"
            placeholder="输入文档内容（支持 Markdown 格式）..."
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tagsStr" placeholder="多个标签用逗号分隔，如: java,spring,api" />
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
    
    <!-- 查看文档详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="文档详情" width="900px" top="5vh">
      <div class="document-view" v-loading="detailLoading">
        <div class="doc-header">
          <h3>{{ currentDocument.title }}</h3>
          <div class="doc-actions">
            <el-button size="small" type="success" @click="downloadDocument(currentDocument)">
              <el-icon><Download /></el-icon>
              下载文档
            </el-button>
          </div>
        </div>
        
        <!-- 元数据 -->
        <div class="doc-meta">
          <el-tag :type="getCategoryTagType(currentDocument.category)" size="small">
            {{ currentDocument.category || '未分类' }}
          </el-tag>
          <el-tag v-for="tag in (currentDocument.tags || [])" :key="tag" size="small"
                  style="margin-left: 4px;">{{ tag }}</el-tag>
        </div>
        
        <el-descriptions :column="2" border size="small" style="margin-top: 16px;">
          <el-descriptions-item label="文档 ID">
            <el-text type="info" size="small">{{ currentDocument.id }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="来源">
            {{ currentDocument.source || '手动添加' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(currentDocument.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="最后访问">
            {{ formatDate(currentDocument.lastAccessedAt) || '从未访问' }}
          </el-descriptions-item>
          <el-descriptions-item label="访问次数">
            {{ currentDocument.accessCount || 0 }} 次
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDate(currentDocument.updatedAt) || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <!-- 文档内容 -->
        <div class="doc-content-label">文档内容</div>
        <div class="document-content">{{ currentDocument.content || '暂无内容' }}</div>
      </div>
      
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
        <el-button type="success" @click="downloadDocument(currentDocument)">
          <el-icon><Download /></el-icon>
          下载
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Search, View, Delete, Download, Refresh } from '@element-plus/icons-vue'
import { knowledge } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const documents = ref([])
const searchQuery = ref('')
const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('添加文档')
const currentDocument = ref({})
const isSearchMode = ref(false)

const form = ref({
  title: '',
  category: '',
  content: '',
  tagsStr: '',
  projectPath: ''
})

onMounted(() => {
  loadDocuments()
})

/** 格式化日期 */
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  if (typeof dateStr === 'string') {
    // 处理 Java LocalDateTime 数组格式 [2026,6,3,14,25,12]
    if (dateStr.startsWith('[')) {
      try {
        const parts = JSON.parse(dateStr)
        if (Array.isArray(parts) && parts.length >= 3) {
          return `${parts[0]}/${String(parts[1]).padStart(2,'0')}/${String(parts[2]).padStart(2,'0')} ${String(parts[3]||0).padStart(2,'0')}:${String(parts[4]||0).padStart(2,'0')}:${String(parts[5]||0).padStart(2,'0')}`
        }
      } catch {}
    }
    return dateStr
  }
  if (Array.isArray(dateStr) && dateStr.length >= 3) {
    return `${dateStr[0]}/${String(dateStr[1]).padStart(2,'0')}/${String(dateStr[2]).padStart(2,'0')} ${String(dateStr[3]||0).padStart(2,'0')}:${String(dateStr[4]||0).padStart(2,'0')}:${String(dateStr[5]||0).padStart(2,'0')}`
  }
  return String(dateStr)
}

/** 分类标签颜色 */
const getCategoryTagType = (category) => {
  const map = {
    'api': 'primary',
    '技术文档': 'success',
    '最佳实践': 'warning',
    '架构设计': 'danger',
    'coding-standards': 'info',
    'best-practices': 'warning',
    'architecture': 'danger',
    'system': ''
  }
  return map[category] || 'info'
}

/** 表格行样式 */
const tableRowClassName = ({ row }) => {
  return row.source === 'system' ? 'system-row' : ''
}

/** 加载文档列表 */
const loadDocuments = async () => {
  loading.value = true
  isSearchMode.value = false
  try {
    const response = await knowledge.listDocuments()
    documents.value = response.documents || []
    if (!documents.value.length) {
      ElMessage.info('知识库暂无文档，请点击"添加文档"创建')
    }
  } catch (error) {
    ElMessage.error('加载文档列表失败: ' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

/** 语义搜索 */
const searchDocuments = async () => {
  if (!searchQuery.value.trim()) {
    loadDocuments()
    return
  }
  
  loading.value = true
  isSearchMode.value = true
  try {
    const response = await knowledge.search(searchQuery.value, 20)
    documents.value = response.results || []
    if (!documents.value.length) {
      ElMessage.info('未找到相关文档')
    } else {
      ElMessage.success(`找到 ${documents.value.length} 个相关文档`)
    }
  } catch (error) {
    ElMessage.error('搜索失败: ' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

/** 显示添加对话框 */
const showAddDialog = () => {
  dialogTitle.value = '添加文档'
  form.value = {
    title: '',
    category: '',
    content: '',
    tagsStr: '',
    projectPath: ''
  }
  dialogVisible.value = true
}

/** 保存文档 */
const saveDocument = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  
  saving.value = true
  try {
    const tags = form.value.tagsStr
      ? form.value.tagsStr.split(/[,，]/).map(t => t.trim()).filter(Boolean)
      : []
    
    await knowledge.addDocument({
      title: form.value.title,
      category: form.value.category || 'other',
      content: form.value.content,
      projectPath: form.value.projectPath,
      tags,
      source: 'manual'
    })
    ElMessage.success('文档保存成功')
    dialogVisible.value = false
    loadDocuments()
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.response?.data?.error || error.message))
  } finally {
    saving.value = false
  }
}

/** 查看文档详情 */
const viewDocument = async (doc) => {
  detailLoading.value = true
  viewDialogVisible.value = true
  currentDocument.value = { ...doc, content: '加载中...' }
  
  try {
    const response = await knowledge.getDocument(doc.id)
    currentDocument.value = response.document || doc
  } catch {
    // 如果详情接口失败，使用列表中的数据
    currentDocument.value = doc
  } finally {
    detailLoading.value = false
  }
}

/** 下载文档 */
const downloadDocument = (doc) => {
  if (!doc.id) {
    ElMessage.warning('文档 ID 不存在')
    return
  }
  const url = knowledge.getDownloadUrl(doc.id)
  // 创建隐藏 a 标签触发下载
  const a = document.createElement('a')
  a.href = url
  a.download = (doc.title || doc.id) + '.md'
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  ElMessage.success(`正在下载: ${doc.title}`)
}

/** 删除文档 */
const deleteDocument = async (doc) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${doc.title}」吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await knowledge.deleteDocument(doc.id)
    if (response.success) {
      ElMessage.success('文档已删除')
      loadDocuments()
    } else {
      ElMessage.error(response.error || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel' && error?.toString() !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.error || error.message))
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

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

/* 文档详情弹窗 */
.document-view {
  max-height: 70vh;
  overflow-y: auto;
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.doc-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--neon-blue, #409eff);
  flex: 1;
}

.doc-actions {
  flex-shrink: 0;
  margin-left: 16px;
}

.doc-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

.doc-content-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  margin: 16px 0 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.document-content {
  padding: 16px;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  font-size: 14px;
  max-height: 50vh;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', monospace;
}

:deep(.el-descriptions) {
  --el-descriptions-item-bordered-label-background: rgba(0, 0, 0, 0.02);
}

:deep(.system-row) {
  opacity: 0.85;
}
</style>
