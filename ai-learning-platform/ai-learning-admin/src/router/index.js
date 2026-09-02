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

      // ================= 教师端（二级分组路由 + 三级功能路由，与管理员端企业级样式一致） =================
      // 教学管理
      {
        path: 'teacher/course',
        component: () => import('../layouts/RouteGroup.vue'),
        children: [
          { path: 'manage', name: 'course', component: () => import('../views/teacher/CourseManageView.vue') },
          { path: 'students', name: 'course-students', component: () => import('../views/course/CourseStudentsView.vue') },
          { path: '', redirect: { name: 'course' } }
        ]
      },
      // 考试管理
      {
        path: 'teacher/exam',
        component: () => import('../layouts/RouteGroup.vue'),
        children: [
          { path: 'question', name: 'question', component: () => import('../views/teacher/QuestionBankView.vue') },
          { path: 'paper', name: 'exam', component: () => import('../views/teacher/ExamManageView.vue') },
          { path: '', redirect: { name: 'question' } }
        ]
      },
      // AI 助教
      {
        path: 'teacher/ai',
        component: () => import('../layouts/RouteGroup.vue'),
        children: [
          { path: 'generate', name: 'ai-generate', component: () => import('../views/teacher/AiGenerateView.vue') },
          { path: 'grade', name: 'ai-grade', component: () => import('../views/teacher/AiGradeView.vue') },
          { path: '', redirect: { name: 'ai-generate' } }
        ]
      },

      // ================= 管理员端（二级分组路由 + 三级功能路由） =================
      // 课程管理
      {
        path: 'admin/course',
        component: () => import('../layouts/RouteGroup.vue'),
        children: [
          { path: 'review', name: 'course-review', component: () => import('../views/admin/CourseReviewView.vue') },
          { path: 'manage', name: 'admin-course', component: () => import('../views/admin/CourseManageAdminView.vue') },
          { path: 'students', name: 'admin-course-students', component: () => import('../views/course/CourseStudentsView.vue') },
          { path: '', redirect: { name: 'course-review' } }
        ]
      },
      // 运营管理
      {
        path: 'admin/ops',
        component: () => import('../layouts/RouteGroup.vue'),
        children: [
          { path: 'notice', name: 'notice', component: () => import('../views/admin/NoticeManageView.vue') },
          { path: 'comment', name: 'comment', component: () => import('../views/admin/CommentManageView.vue') },
          { path: '', redirect: { name: 'notice' } }
        ]
      },
      // 积分管理
      {
        path: 'admin/points',
        component: () => import('../layouts/RouteGroup.vue'),
        children: [
          { path: 'rule', name: 'points-rule', component: () => import('../views/admin/PointsRuleView.vue') },
          { path: 'activity', name: 'points-activity', component: () => import('../views/admin/PointsActivityView.vue') },
          { path: 'exchange', name: 'exchange-record', component: () => import('../views/admin/ExchangeRecordView.vue') },
          { path: '', redirect: { name: 'points-rule' } }
        ]
      },
      // 用户管理（二级分组路由：角色分类 → 三级功能页）
      {
        path: 'admin/user',
        component: () => import('../layouts/RouteGroup.vue'),
        children: [
          { path: 'student', name: 'user-student', component: () => import('../views/admin/user/UserStudentView.vue') },
          { path: 'teacher', name: 'user-teacher', component: () => import('../views/admin/user/UserTeacherView.vue') },
          { path: 'manager', name: 'user-manager', component: () => import('../views/admin/user/UserManagerAccountView.vue') },
          { path: '', redirect: { name: 'user-student' } }
        ]
      },

      // 系统设置（管理员）
      { path: 'admin/settings', name: 'settings', component: () => import('../views/admin/SystemSettingsView.vue') },

      // ================= 个人中心（教师/管理员共用） =================
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