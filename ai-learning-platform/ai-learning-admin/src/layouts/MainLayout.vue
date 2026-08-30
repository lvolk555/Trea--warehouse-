<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  DashboardOutlined, BookOutlined, DatabaseOutlined, FormOutlined,
  RobotOutlined, BarChartOutlined, NotificationOutlined, MessageOutlined,
  GiftOutlined, TeamOutlined, SettingOutlined, LogoutOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

// 侧边菜单：按角色动态渲染（教师菜单 / 管理员菜单），key 与路由 name 对应
const teacherMenus = [
  { key: 'Dashboard', label: '工作台', icon: DashboardOutlined },
  { key: 'course', label: '课程管理', icon: BookOutlined },
  { key: 'question', label: '题库管理', icon: DatabaseOutlined },
  { key: 'exam', label: '组卷考试', icon: FormOutlined },
  { key: 'ai-tools', label: 'AI 出题/批改', icon: RobotOutlined },
  { key: 'learning-stats', label: '学情分析', icon: BarChartOutlined }
]
const adminMenus = [
  { key: 'Dashboard', label: '数据看板', icon: DashboardOutlined },
  { key: 'course-review', label: '课程审核', icon: BookOutlined },
  { key: 'admin-course', label: '课程管理', icon: DatabaseOutlined },
  { key: 'notice', label: '公告管理', icon: NotificationOutlined },
  { key: 'comment', label: '评论管理', icon: MessageOutlined },
  { key: 'points-rule', label: '积分规则', icon: GiftOutlined },
  { key: 'exchange-record', label: '兑换记录', icon: DatabaseOutlined },
  { key: 'user-manage', label: '用户管理', icon: TeamOutlined },
  { key: 'settings', label: '系统设置', icon: SettingOutlined }
]

const menus = computed(() => (userStore.isAdmin ? adminMenus : teacherMenus))
const roleText = computed(() => (userStore.isAdmin ? '管理员' : '教师'))

// 菜单点击跳转（未开发的模块给出提示）
const developedRoutes = ['Dashboard', 'course', 'course-review', 'admin-course', 'question', 'exam']
function handleMenuClick({ key }) {
  if (developedRoutes.includes(key)) {
    router.push({ name: key })
  } else {
    message.info('该模块将在后续阶段开发')
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
    <!-- 侧边菜单 -->
    <a-layout-sider width="220" theme="dark">
      <div class="sider-brand">
        <div class="logo">AI</div>
        <span>学习平台管理端</span>
      </div>
      <a-menu theme="dark" mode="inline" :selected-keys="[$route.name]" @click="handleMenuClick">
        <a-menu-item v-for="m in menus" :key="m.key">
          <component :is="m.icon" />
          <span>{{ m.label }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <!-- 顶部栏 -->
      <a-layout-header class="header">
        <span class="page-title">{{ $route.name === 'Dashboard' ? (userStore.isAdmin ? '数据看板' : '工作台') : '' }}</span>
        <div class="user-area">
          <a-tag color="blue">{{ roleText }}</a-tag>
          <a-dropdown>
            <span class="username">{{ userStore.user?.nickname || userStore.user?.username }}</span>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <!-- 内容区 -->
      <a-layout-content style="margin: 24px">
        <router-view />
      </a-layout-content>
    </a-layout>
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
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
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
</style>
