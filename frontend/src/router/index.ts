import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

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
    meta: { title: '案件推演' }
  },
  {
    path: '/workspace/:bookId/graph/:graphId',
    name: 'RelationGraph',
    component: () => import('@/views/RelationGraphView.vue'),
    meta: { title: '关系图' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/bookshelf'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title ?? '推演录'} · 推演录`
  next()
})

export default router
