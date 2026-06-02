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
    path: '/code-editor',
    name: 'CodeEditor',
    component: () => import('../views/CodeEditorView.vue')
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
    path: '/learning',
    name: 'Learning',
    component: () => import('../views/LearningView.vue')
  },
  {
    path: '/supervisor',
    name: 'Supervisor',
    component: () => import('../views/SupervisorView.vue')
  },
  {
    path: '/projects',
    name: 'Projects',
    component: () => import('../views/ProjectsView.vue')
  },
  {
    path: '/monitoring',
    name: 'Monitoring',
    component: () => import('../views/MonitoringView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
