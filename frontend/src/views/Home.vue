<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h1>欢迎使用 AI 编程助手</h1>
        </div>
      </template>
      <div class="features-grid">
        <el-card class="feature-card" shadow="hover" @click="$router.push('/chat')">
          <el-icon :size="50" color="#409eff"><ChatDotRound /></el-icon>
          <h3>AI 对话</h3>
          <p>与 AI 助手对话，获取编程建议和代码生成</p>
        </el-card>
        
        <el-card class="feature-card" shadow="hover" @click="$router.push('/code-editor')">
          <el-icon :size="50" color="#67c23a"><Edit /></el-icon>
          <h3>代码编辑器</h3>
          <p>可视化编辑代码，AI 辅助编程</p>
        </el-card>
        
        <el-card class="feature-card" shadow="hover" @click="$router.push('/knowledge')">
          <el-icon :size="50" color="#e6a23c"><Collection /></el-icon>
          <h3>知识库管理</h3>
          <p>管理项目知识，支持 RAG 检索增强</p>
        </el-card>
        
        <el-card class="feature-card" shadow="hover" @click="$router.push('/skills')">
          <el-icon :size="50" color="#f56c6c"><Star /></el-icon>
          <h3>技能管理</h3>
          <p>配置和管理 AI 技能包</p>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ChatDotRound, Edit, Collection, Star } from '@element-plus/icons-vue'
</script>

<style scoped>
.home-container {
  padding: 20px;
}

.welcome-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header h1 {
  margin: 0;
  color: #303133;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.feature-card {
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s;
}

.feature-card:hover {
  transform: translateY(-5px);
}

.feature-card h3 {
  margin: 15px 0 10px;
  color: #303133;
}

.feature-card p {
  color: #909399;
  font-size: 14px;
}
</style>
