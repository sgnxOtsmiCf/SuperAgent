import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue')
  },
  {
    path: '/model-list',
    name: 'ModelList',
    component: () => import('@/views/ModelListView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
