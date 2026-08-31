import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/auth/LoginView.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/auth/RegisterView.vue') },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', redirect: '/square' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/StudentDashboardView.vue') },
      { path: 'square', name: 'Square', component: () => import('../views/course/SquareView.vue') },
      { path: 'course/:courseId', name: 'CourseDetail', component: () => import('../views/course/DetailView.vue') },
      { path: 'my-courses', name: 'MyCourses', component: () => import('../views/course/MyCoursesView.vue') },
      { path: 'study/:courseId/:videoId', name: 'Study', component: () => import('../views/study/StudyView.vue') },
      { path: 'notes', name: 'Notes', component: () => import('../views/study/NotesView.vue') },
      // 练习与考试
      { path: 'practice', name: 'Practice', component: () => import('../views/practice/PracticeView.vue') },
      { path: 'error-book', name: 'ErrorBook', component: () => import('../views/practice/ErrorBookView.vue') },
      { path: 'exam', name: 'ExamList', component: () => import('../views/exam/ExamListView.vue') },
      { path: 'exam/:examId', name: 'ExamTake', component: () => import('../views/exam/ExamTakeView.vue') },
      { path: 'exam-result/:recordId', name: 'ExamResult', component: () => import('../views/exam/ExamResultView.vue') },
      { path: 'scores', name: 'Scores', component: () => import('../views/exam/ScoreListView.vue') },
      // AI 答疑
      { path: 'ai-chat', name: 'AiChat', component: () => import('../views/ai/AiChatView.vue') },
      // 积分中心与公告
      { path: 'points', name: 'PointsCenter', component: () => import('../views/points/PointsCenterView.vue') },
      { path: 'notices', name: 'Notices', component: () => import('../views/notice/NoticeListView.vue') },
      // 个人中心
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
  if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    return '/'
  }
})

export default router
