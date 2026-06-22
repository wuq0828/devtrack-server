import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
  },
  {
    path: '/defects',
    name: 'defects',
    component: () => import('@/views/DefectListView.vue'),
  },
  {
    path: '/board',
    name: 'board',
    component: () => import('@/views/DefectBoardView.vue'),
  },
  {
    path: '/iterations',
    name: 'iterations',
    component: () => import('@/views/IterationView.vue'),
  },
  {
    path: '/requirements',
    name: 'requirements',
    component: () => import('@/views/RequirementView.vue'),
  },
  {
    path: '/testcases',
    name: 'testcases',
    component: () => import('@/views/TestCaseView.vue'),
  },
  {
    path: '/ai-cases',
    name: 'ai-cases',
    component: () => import('@/views/AiCasesView.vue'),
  },
  {
    path: '/testruns',
    name: 'testruns',
    component: () => import('@/views/TestRunView.vue'),
  },
  {
    path: '/coverage',
    name: 'coverage',
    component: () => import('@/views/CoverageView.vue'),
  },
  {
    path: '/gantt',
    name: 'gantt',
    component: () => import('@/views/GanttView.vue'),
  },
  {
    path: '/logs',
    name: 'logs',
    component: () => import('@/views/ObservLogView.vue'),
  },
  {
    path: '/',
    redirect: '/defects',
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/defects',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// ---- Auth guard ----
router.beforeEach((to) => {
  const token = localStorage.getItem('devtrack_token')
  const isPublic = to.meta.public === true

  if (!token && !isPublic) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // If already logged in, keep users off the login page.
  if (token && to.path === '/login') {
    return { path: '/defects' }
  }
  return true
})

export default router
