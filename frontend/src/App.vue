<template>
  <el-container class="app-container">
    <!-- 粒子背景 -->
    <div class="particles-bg">
      <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
    </div>
    
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <div class="logo-icon">🤖</div>
        <h2 class="neon-text">AI 编程助手</h2>
        <div class="logo-line"></div>
      </div>
      
      <el-menu
        :default-active="$route.path"
        router
        class="tech-menu"
      >
        <el-menu-item index="/" class="menu-item">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
          <div class="menu-glow"></div>
        </el-menu-item>
        <el-menu-item index="/chat" class="menu-item">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 对话</span>
          <div class="menu-glow"></div>
        </el-menu-item>
        <el-menu-item index="/code-editor" class="menu-item">
          <el-icon><Edit /></el-icon>
          <span>代码编辑器</span>
          <div class="menu-glow"></div>
        </el-menu-item>
        <el-menu-item index="/knowledge" class="menu-item">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
          <div class="menu-glow"></div>
        </el-menu-item>
        <el-menu-item index="/skills" class="menu-item">
          <el-icon><Star /></el-icon>
          <span>技能管理</span>
          <div class="menu-glow"></div>
        </el-menu-item>
      </el-menu>
      
      <!-- 底部状态 -->
      <div class="sidebar-footer">
        <div class="status-dot"></div>
        <span>系统运行中</span>
      </div>
    </el-aside>
    
    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { HomeFilled, ChatDotRound, Edit, Collection, Star } from '@element-plus/icons-vue'

const getParticleStyle = (index) => {
  return {
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    animationDelay: `${Math.random() * 5}s`,
    animationDuration: `${5 + Math.random() * 5}s`
  }
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 粒子背景 */
.particles-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: var(--neon-blue);
  border-radius: 50%;
  box-shadow: 0 0 10px var(--neon-blue);
  animation: float 10s infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) translateX(0);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) translateX(50px);
    opacity: 0;
  }
}

/* 侧边栏 */
.sidebar {
  background: linear-gradient(180deg, rgba(10, 14, 39, 0.95) 0%, rgba(5, 8, 17, 0.98) 100%);
  border-right: 1px solid var(--border-glow);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background: linear-gradient(180deg, var(--neon-blue), var(--neon-purple), var(--neon-pink));
  animation: borderGlow 3s ease-in-out infinite;
}

@keyframes borderGlow {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.logo {
  padding: 30px 20px;
  text-align: center;
  position: relative;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 10px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.logo h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.logo-line {
  margin-top: 15px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--neon-blue), transparent);
  animation: lineSweep 2s ease-in-out infinite;
}

@keyframes lineSweep {
  0%, 100% { opacity: 0.5; transform: scaleX(0.8); }
  50% { opacity: 1; transform: scaleX(1); }
}

/* 菜单样式 */
.tech-menu {
  flex: 1;
  background: transparent;
  border: none;
  padding: 20px 0;
}

.menu-item {
  margin: 8px 16px;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  color: var(--text-secondary);
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(0, 242, 254, 0.1);
  color: var(--neon-blue);
}

.menu-item.is-active {
  background: linear-gradient(135deg, rgba(0, 242, 254, 0.2), rgba(178, 36, 239, 0.2));
  color: var(--neon-blue);
  box-shadow: 0 0 20px rgba(0, 242, 254, 0.3);
}

.menu-glow {
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, var(--neon-blue), var(--neon-purple));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.menu-item.is-active .menu-glow {
  opacity: 1;
  box-shadow: 0 0 10px var(--neon-blue);
}

/* 底部状态 */
.sidebar-footer {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid var(--border-glow);
  color: var(--text-secondary);
  font-size: 14px;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 10px #10b981;
  animation: blink 2s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 主内容区 */
.main-content {
  padding: 0;
  background: transparent;
  position: relative;
  z-index: 1;
}

/* Element UI 覆盖 */
:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item) {
  background: transparent;
}
</style>
