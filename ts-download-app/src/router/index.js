import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../components/LoginPage.vue'),
    meta: { public: true }
  },
  {
    path: '/download',
    name: 'Download',
    component: () => import('../components/DownloadPage.vue')
  },
  {
    path: '/admin/users',
    name: 'UserManage',
    component: () => import('../components/UserManagePage.vue'),
    meta: { requireAdmin: true }
  },
  {
    path: '/logs',
    name: 'DownloadLog',
    component: () => import('../components/DownloadLogPage.vue'),
    meta: { requireAdmin: true }
  },
  {
    path: '/loginLogs',
    name: 'LoginLog',
    component: () => import('../components/LoginLogPage.vue'),
    meta: { requireAdmin: true }
  },
  {
    path: '/validUsers',
    name: 'ValidUser',
    component: () => import('../components/ValidUserPage.vue'),
    meta: { requireLeader: true }
  },
  {
    path: '/',
    redirect: '/download'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = JSON.parse(sessionStorage.getItem('currentUser') || 'null')

  if (to.meta.public) {
    next()
    return
  }

  if (!user) {
    next('/login')
    return
  }

  if (to.meta.requireAdmin && user.role !== 'admin' && user.role !== 'superAdmin') {
    next('/download')
    return
  }

  if (to.meta.requireLeader && user.role !== 'leader' && user.role !== 'admin' && user.role !== 'superAdmin') {
    next('/download')
    return
  }

  next()
})

export default router
