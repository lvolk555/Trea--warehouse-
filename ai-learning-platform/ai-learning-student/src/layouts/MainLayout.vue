<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { useUserStore } from '../stores/user'
import { pointsAccount } from '../api/points'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

// 移动端适配：窄屏隐藏侧边栏，改用抽屉导航
const isMobile = ref(false)
const drawerVisible = ref(false)

function checkWidth() {
  isMobile.value = window.innerWidth <= 768
}

// 顶部实时显示积分余额
const pointsBalance = ref('--')
async function loadPoints() {
  try {
    const acc = await pointsAccount()
    pointsBalance.value = acc.balance
  } catch (e) {
    /* 未登录或接口异常时保持占位 */
  }
}
onMounted(() => {
  loadPoints()
  checkWidth()
  window.addEventListener('resize', checkWidth)
})
onUnmounted(() => {
  window.removeEventListener('resize', checkWidth)
})

// 学生端侧边导航
const menus = [
  { key: 'Dashboard', label: '学习看板' },
  { key: 'Square', label: '课程广场' },
  { key: 'MyCourses', label: '我的课程' },
  { key: 'Notes', label: '学习笔记' },
  { key: 'Practice', label: '章节练习' },
  { key: 'ExamList', label: '在线考试' },
  { key: 'ErrorBook', label: '错题本' },
  { key: 'Scores', label: '我的成绩' },
  { key: 'AiChat', label: 'AI 答疑' },
  { key: 'PointsCenter', label: '积分中心' },
  { key: 'Notices', label: '公告' }
]

const menuOptions = computed(() => menus.map(m => ({ key: m.key, label: m.label })))

const selectedKey = computed(() => {
  const name = route.name
  if (name === 'CourseDetail' || name === 'Study') return 'Square'
  if (name === 'ExamTake' || name === 'ExamResult') return 'ExamList'
  return name
})

const currentTitle = computed(() => {
  const item = menus.find(m => m.key === selectedKey.value)
  return item ? item.label : ''
})

function handleMenuClick(key) {
  drawerVisible.value = false
  router.push({ name: key })
}

function handleLogout() {
  dialog.warning({
    title: '退出登录',
    content: '确定要退出登录吗？',
    positiveText: '退出',
    negativeText: '取消',
    onPositiveClick: () => {
      userStore.logout()
      message.success('已退出登录')
      router.push('/login')
    }
  })
}

// 顶部用户下拉菜单
const userMenuOptions = [
  { key: 'profile', label: '个人中心' },
  { key: 'logout', label: '退出登录' }
]
function handleUserMenu(key) {
  if (key === 'profile') {
    router.push({ name: 'Profile' })
  } else if (key === 'logout') {
    handleLogout()
  }
}
</script>

<template>
  <n-layout has-sider style="height: 100vh">
    <!-- 侧边导航（桌面端） -->
    <n-layout-sider v-if="!isMobile" bordered width="220">
      <div class="sider-brand">
        <div class="logo">AI</div>
        <span>AI 辅助学习平台</span>
      </div>
      <n-menu :value="selectedKey" :options="menuOptions" @update:value="handleMenuClick" />
    </n-layout-sider>

    <n-layout style="display: flex; flex-direction: column">
      <!-- 顶部栏 -->
      <n-layout-header bordered class="top-header">
        <div class="header-left">
          <n-button v-if="isMobile" quaternary class="menu-btn" @click="drawerVisible = true">☰</n-button>
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="user-area">
          <n-tag type="warning" size="small" round style="cursor: pointer" @click="router.push('/points')">积分 {{ pointsBalance }}</n-tag>
          <n-dropdown :options="userMenuOptions" @select="handleUserMenu">
            <n-button quaternary>
              {{ userStore.user?.nickname || userStore.user?.username }}
            </n-button>
          </n-dropdown>
        </div>
      </n-layout-header>
      <!-- 内容区 -->
      <n-layout-content class="main-content">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>

  <!-- 移动端抽屉导航 -->
  <n-drawer v-model:show="drawerVisible" placement="left" :width="240">
    <n-drawer-content title="AI 辅助学习平台" closable>
      <n-menu :value="selectedKey" :options="menuOptions" @update:value="handleMenuClick" />
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
.sider-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
  color: #1f2937;
}
.logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
.top-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.menu-btn {
  font-size: 20px;
  line-height: 1;
  padding: 4px 10px;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.main-content {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}
@media (max-width: 768px) {
  .top-header {
    padding: 0 12px;
  }
  .user-area {
    gap: 6px;
  }
  .main-content {
    padding: 12px;
  }
}
</style>