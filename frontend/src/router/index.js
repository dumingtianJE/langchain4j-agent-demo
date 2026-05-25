import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/ChatView.vue')
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('../views/KnowledgeView.vue')
  },
  {
    path: '/skills',
    name: 'Skills',
    component: () => import('../views/SkillsView.vue')
  },
  {
    path: '/code-editor',
    name: 'CodeEditor',
    component: () => import('../views/CodeEditorView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
