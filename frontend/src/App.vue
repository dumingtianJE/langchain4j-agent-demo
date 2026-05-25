<template>
  <el-container class="app-container">
    <el-aside width="200px">
      <div class="logo">
        <h2>🤖 AI 编程助手</h2>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 对话</span>
        </el-menu-item>
        <el-menu-item index="/code-editor">
          <el-icon><Edit /></el-icon>
          <span>代码编辑器</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/skills">
          <el-icon><Star /></el-icon>
          <span>技能管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { HomeFilled, ChatDotRound, Edit, Collection, Star } from '@element-plus/icons-vue'
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.el-aside {
  background-color: #304156;
  color: #fff;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #3d4f6f;
}

.logo h2 {
  margin: 0;
  color: #fff;
  font-size: 18px;
}

.el-menu {
  border-right: none;
}

.el-main {
  padding: 0;
  background-color: #f5f7fa;
}
</style>
