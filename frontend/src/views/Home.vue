<template>
  <div class="home-container fade-in">
    <!-- 顶部欢迎区域 -->
    <div class="welcome-banner tech-card scan-line">
      <h1 class="welcome-title neon-text">欢迎使用 AI 编程助手</h1>
      <p class="welcome-subtitle">下一代智能编程体验 · 让代码创作更轻松</p>
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-value">{{ stats.skills }}</div>
          <div class="stat-label">技能数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.documents }}</div>
          <div class="stat-label">知识库文档</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.conversations }}</div>
          <div class="stat-label">对话次数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">99.9%</div>
          <div class="stat-label">系统可用率</div>
        </div>
      </div>
    </div>
    
    <!-- 功能卡片网格 -->
    <div class="features-grid">
      <div 
        v-for="(feature, index) in features" 
        :key="index"
        class="feature-card tech-card"
        @click="$router.push(feature.path)"
        :style="{ animationDelay: `${index * 0.1}s` }"
      >
        <div class="feature-icon" :style="{ background: feature.gradient }">
          <el-icon :size="40"><component :is="feature.icon" /></el-icon>
        </div>
        <h3>{{ feature.title }}</h3>
        <p>{{ feature.description }}</p>
        <div class="feature-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="card-corner corner-tl"></div>
        <div class="card-corner corner-br"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ChatDotRound, Edit, Collection, Star, ArrowRight } from '@element-plus/icons-vue'

const stats = ref({
  skills: 12,
  documents: 156,
  conversations: 1024
})

const features = [
  {
    icon: ChatDotRound,
    title: 'AI 对话',
    description: '与 AI 助手对话，获取编程建议和代码生成',
    path: '/chat',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    icon: Edit,
    title: '代码编辑器',
    description: '可视化编辑代码，AI 辅助编程',
    path: '/code-editor',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    icon: Collection,
    title: '知识库管理',
    description: '管理项目知识，支持 RAG 检索增强',
    path: '/knowledge',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    icon: Star,
    title: '技能管理',
    description: '配置和管理 AI 技能包',
    path: '/skills',
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
]
</script>

<style scoped>
.home-container {
  padding: 40px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 顶部欢迎区域 */
.welcome-banner {
  padding: 60px 40px;
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  overflow: hidden;
}

.welcome-title {
  font-size: 48px;
  font-weight: 800;
  margin-bottom: 16px;
  letter-spacing: 2px;
}

.welcome-subtitle {
  font-size: 20px;
  color: var(--text-secondary);
  margin-bottom: 40px;
}

/* 统计数据 */
.stats-row {
  display: flex;
  justify-content: space-around;
  gap: 20px;
  margin-top: 40px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--neon-blue), var(--neon-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* 功能卡片网格 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 30px;
}

.feature-card {
  padding: 40px 30px;
  text-align: center;
  cursor: pointer;
  position: relative;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeIn 0.6s ease-out backwards;
}

.feature-card:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow: 
    0 20px 60px rgba(0, 242, 254, 0.3),
    0 0 40px rgba(0, 242, 254, 0.2);
}

.feature-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  color: white;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  transition: transform 0.3s ease;
}

.feature-card:hover .feature-icon {
  transform: scale(1.1) rotate(5deg);
}

.feature-card h3 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.feature-card p {
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 20px;
}

.feature-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 242, 254, 0.1);
  color: var(--neon-blue);
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s ease;
}

.feature-card:hover .feature-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* 卡片装饰角 */
.card-corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border: 2px solid var(--neon-blue);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.corner-tl {
  top: 10px;
  left: 10px;
  border-right: none;
  border-bottom: none;
}

.corner-br {
  bottom: 10px;
  right: 10px;
  border-left: none;
  border-top: none;
}

.feature-card:hover .card-corner {
  opacity: 0.6;
}

/* 响应式 */
@media (max-width: 768px) {
  .home-container {
    padding: 20px;
  }
  
  .welcome-title {
    font-size: 32px;
  }
  
  .stats-row {
    flex-wrap: wrap;
  }
  
  .stat-value {
    font-size: 28px;
  }
}
</style>
