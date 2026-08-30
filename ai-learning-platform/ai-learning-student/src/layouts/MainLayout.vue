<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

// 学生端顶部导航（阶段二起逐步补充菜单项）
const menus = [
  { key: 'Square', label: '课程广场' },
  { key: 'MyCourses', label: '我的课程' },
  { key: 'Practice', label: '章节练习' },
  { key: 'ExamList', label: '在线考试' },
  { key: 'ErrorBook', label: '错题本' },
  { key: 'Scores', label: '我的成绩' }
]

const selectedKey = computed(() => {
  const name = route.name
  if (name === 'CourseDetail' || name === 'Study') return 'Square'
  if (name === 'ExamTake' || name === 'ExamResult') return 'ExamList'
  return name
})

function handleMenuClick(key) {
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
</script>

<template>
  <n-layout has-sider style="height: 100vh">
    <n-layout style="display: flex; flex-direction: column">
      <!-- 顶部导航栏 -->
      <n-layout-header bordered style="height: 60px; display: flex; align-items: center; padding: 0 24px; background: #fff">
        <div class="brand">
          <div class="logo">AI</div>
          <span>AI 辅助在线学习平台</span>
        </div>
        <n-menu mode="horizontal" :value="selectedKey" :options="menus.map(m => ({ key: m.key, label: m.label }))" style="flex: 1" @update:value="handleMenuClick" />
        <div class="user-area">
          <n-tag type="warning" size="small" round>积分 --</n-tag>
          <n-dropdown :options="[{ key: 'logout', label: '退出登录' }]" @select="handleLogout">
            <n-button quaternary>
              {{ userStore.user?.nickname || userStore.user?.username }}
            </n-button>
          </n-dropdown>
        </div>
      </n-layout-header>
      <!-- 内容区 -->
      <n-layout-content content-style="padding: 24px; max-width: 1200px; margin: 0 auto; width: 100%;">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<style scoped>
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 16px;
  margin-right: 24px;
  white-space: nowrap;
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
}
.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
