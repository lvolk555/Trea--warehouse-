import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/LoginView.vue') },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('../views/dashboard/DashboardView.vue') },
      // 教师：课程管理
      { path: 'teacher/course', name: 'course', component: () => import('../views/teacher/CourseManageView.vue') },
      // 教师：题库管理、组卷考试
      { path: 'teacher/question', name: 'question', component: () => import('../views/teacher/QuestionBankView.vue') },
      { path: 'teacher/exam', name: 'exam', component: () => import('../views/teacher/ExamManageView.vue') },
      // 教师：AI 出题、AI 批改
      { path: 'teacher/ai-generate', name: 'ai-generate', component: () => import('../views/teacher/AiGenerateView.vue') },
      { path: 'teacher/ai-grade', name: 'ai-grade', component: () => import('../views/teacher/AiGradeView.vue') },
      // 管理员：课程审核、课程管理
      { path: 'admin/course-review', name: 'course-review', component: () => import('../views/admin/CourseReviewView.vue') },
      { path: 'admin/course', name: 'admin-course', component: () => import('../views/admin/CourseManageAdminView.vue') },
      // 管理员：运营（公告/评论/积分规则/兑换记录/用户管理）
      { path: 'admin/notice', name: 'notice', component: () => import('../views/admin/NoticeManageView.vue') },
      { path: 'admin/comment', name: 'comment', component: () => import('../views/admin/CommentManageView.vue') },
      { path: 'admin/points-rule', name: 'points-rule', component: () => import('../views/admin/PointsRuleView.vue') },
      { path: 'admin/points-activity', name: 'points-activity', component: () => import('../views/admin/PointsActivityView.vue') },
      { path: 'admin/exchange-record', name: 'exchange-record', component: () => import('../views/admin/ExchangeRecordView.vue') },
      { path: 'admin/user', name: 'user-manage', component: () => import('../views/admin/UserManageView.vue') },
      // 个人中心（教师/管理员共用）
      { path: 'profile', name: 'Profile', component: () => import('../views/profile/ProfileView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && userStore.isLoggedIn) {
    return '/'
  }
})

export default router
