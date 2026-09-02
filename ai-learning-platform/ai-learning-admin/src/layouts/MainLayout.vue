<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  DashboardOutlined, BookOutlined, FormOutlined,
  RobotOutlined, BarChartOutlined, NotificationOutlined, MessageOutlined,
  GiftOutlined, TeamOutlined, SettingOutlined, LogoutOutlined, UserOutlined,
  MenuOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

// 移动端适配：<=768px 时收起侧边栏，改用抽屉导航
const isMobile = ref(false)
const drawerVisible = ref(false)

function checkWidth() {
  isMobile.value = window.innerWidth <= 768
}

onMounted(() => {
  checkWidth()
  window.addEventListener('resize', checkWidth)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkWidth)
})

// 侧边菜单：按角色动态渲染（教师菜单 / 管理员菜单），key 与路由 name 对应
// 教师菜单：二级分组（展开显示三级相关功能），与管理员端企业级样式一致
const teacherMenus = [
  { key: 'Dashboard', label: '工作台', icon: DashboardOutlined },
  { key: 'course-group', label: '教学管理', icon: BookOutlined, children: [
    { key: 'course', label: '课程管理' },
    { key: 'course-students', label: '学生管理' }
  ] },
  { key: 'exam-group', label: '考试管理', icon: FormOutlined, children: [
    { key: 'question', label: '题库管理' },
    { key: 'exam', label: '组卷考试' }
  ] },
  { key: 'ai-group', label: 'AI 助教', icon: RobotOutlined, children: [
    { key: 'ai-generate', label: 'AI 智能出题' },
    { key: 'ai-grade', label: 'AI 智能批改' }
  ] },
  { key: 'learning-stats', label: '学情分析', icon: BarChartOutlined }
]
// 管理员菜单：二级分组（展开显示三级相关功能）
const adminMenus = [
  { key: 'Dashboard', label: '数据看板', icon: DashboardOutlined },
  { key: 'course-group', label: '课程管理', icon: BookOutlined, children: [
    { key: 'course-review', label: '课程审核' },
    { key: 'admin-course', label: '课程管理' },
    { key: 'admin-course-students', label: '学生管理' }
  ] },
  { key: 'ops-group', label: '运营管理', icon: NotificationOutlined, children: [
    { key: 'notice', label: '公告管理' },
    { key: 'comment', label: '评论管理' }
  ] },
  { key: 'points-group', label: '积分管理', icon: GiftOutlined, children: [
    { key: 'points-rule', label: '积分规则' },
    { key: 'points-activity', label: '积分活动' },
    { key: 'exchange-record', label: '兑换记录' }
  ] },
  { key: 'user-manage', label: '用户管理', icon: TeamOutlined },
  { key: 'settings', label: '系统设置', icon: SettingOutlined }
]

const menus = computed(() => (userStore.isAdmin ? adminMenus : teacherMenus))
const roleText = computed(() => (userStore.isAdmin ? '管理员' : '教师'))

// 分组默认展开（教师与管理员的分组菜单均默认展开）
const openKeys = ref([...teacherMenus, ...adminMenus].filter(m => m.children).map(m => m.key))
function onOpenChange(keys) {
  openKeys.value = keys
}

// 菜单点击跳转（未开发的模块给出提示）
const developedRoutes = ['Dashboard', 'course', 'course-students', 'course-review', 'admin-course', 'admin-course-students', 'question', 'exam', 'ai-generate', 'ai-grade', 'notice', 'comment', 'points-rule', 'points-activity', 'exchange-record', 'user-manage', 'settings']
function handleMenuClick({ key }) {
  if (developedRoutes.includes(key)) {
    router.push({ name: key })
    drawerVisible.value = false
  } else {
    message.info('该模块将在后续阶段开发')
  }
}

// 顶部用户下拉：个人中心 / 退出登录
function handleUserMenu({ key }) {
  if (key === 'profile') {
    router.push({ name: 'Profile' })
  } else if (key === 'logout') {
    handleLogout()
  }
}

function handleLogout() {
  Modal.confirm({
    title: '退出登录',
    content: '确定要退出登录吗？',
    okText: '退出',
    cancelText: '取消',
    onOk: () => {
      userStore.logout()
      message.success('已退出登录')
      router.push('/login')
    }
  })
}
</script>

<template>
  <a-layout style="min-height: 100vh">
    <!-- 侧边菜单（桌面端显示） -->
    <a-layout-sider v-if="!isMobile" width="220" theme="dark">
      <div class="sider-brand">
        <div class="logo">AI</div>
        <span>学习平台管理端</span>
      </div>
      <a-menu theme="dark" mode="inline" :selected-keys="[$route.name]" :open-keys="openKeys" @openChange="onOpenChange" @click="handleMenuClick">
        <template v-for="m in menus" :key="m.key">
          <a-sub-menu v-if="m.children" :key="m.key">
            <template #icon><component :is="m.icon" /></template>
            <template #title>{{ m.label }}</template>
            <a-menu-item v-for="child in m.children" :key="child.key">{{ child.label }}</a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="m.key">
            <component :is="m.icon" />
            <span>{{ m.label }}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <!-- 顶部栏 -->
      <a-layout-header class="header">
        <div class="header-left">
          <a-button v-if="isMobile" type="text" class="menu-trigger" @click="drawerVisible = true">
            <MenuOutlined />
          </a-button>
          <span class="page-title">{{ $route.name === 'Dashboard' ? (userStore.isAdmin ? '数据看板' : '工作台') : '' }}</span>
        </div>
        <div class="user-area">
          <a-tag color="blue">{{ roleText }}</a-tag>
          <a-dropdown>
            <span class="username">{{ userStore.user?.nickname || userStore.user?.username }}</span>
            <template #overlay>
              <a-menu @click="handleUserMenu">
                <a-menu-item key="profile">
                  <UserOutlined /> 个人中心
                </a-menu-item>
                <a-menu-item key="logout">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <!-- 内容区 -->
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>

    <!-- 移动端抽屉导航 -->
    <a-drawer
      v-model:open="drawerVisible"
      placement="left"
      :width="240"
      :closable="false"
      :body-style="{ padding: 0, background: '#001529' }"
      class="mobile-drawer"
    >
      <div class="sider-brand">
        <div class="logo">AI</div>
        <span>学习平台管理端</span>
      </div>
      <a-menu theme="dark" mode="inline" :selected-keys="[$route.name]" :open-keys="openKeys" @openChange="onOpenChange" @click="handleMenuClick">
        <template v-for="m in menus" :key="m.key">
          <a-sub-menu v-if="m.children" :key="m.key">
            <template #icon><component :is="m.icon" /></template>
            <template #title>{{ m.label }}</template>
            <a-menu-item v-for="child in m.children" :key="child.key">{{ child.label }}</a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="m.key">
            <component :is="m.icon" />
            <span>{{ m.label }}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-drawer>
  </a-layout>
</template>

<style scoped>
.sider-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  color: #fff;
  font-weight: 600;
  white-space: nowrap;
}
.logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}
.username {
  cursor: pointer;
}
.menu-trigger {
  color: #333;
  font-size: 18px;
}
.content {
  margin: 24px;
}

@media (max-width: 768px) {
  .header {
    padding: 0 12px;
    height: 48px;
    line-height: 48px;
  }
  .page-title {
    font-size: 15px;
  }
  .user-area {
    gap: 8px;
  }
  .content {
    margin: 12px;
  }
}
</style>