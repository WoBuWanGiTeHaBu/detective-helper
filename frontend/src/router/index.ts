import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/bookshelf'
  },
  {
    path: '/bookshelf',
    name: 'Bookshelf',
    component: () => import('@/views/BookshelfView.vue'),
    meta: { title: '案件书架' }
  },
  {
    path: '/workspace/:bookId',
    name: 'Workspace',
    component: () => import('@/views/BookWorkspaceView.vue'),
    meta: { title: '案件工作空间' }
  },
  {
    path: '/relation-graphs',
    name: 'RelationGraphs',
    component: () => import('@/views/RelationGraphView.vue'),
    meta: { title: '关系图谱' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '侦探助手'} - Detective Helper`
  next()
})

export default router