# 前端代码模板（Vue 3 + Vite 多端工程，通用型）

> 与业务领域无关。按用户群拆独立前端工程，UI 库可按端选择（示例：管理端 Ant Design Vue、用户端 Naive UI）。所有片段可直接复制替换。

## 一、工程结构

```
src/
├── api/            # 按后端模块分文件：ops.js / product.js / order.js
├── views/          # 页面，目录按"角色/功能域"两级组织
│   └── admin/user/ # 同类多页：UserList.vue（共用组件）+ 多个薄壳视图
├── components/     # 跨页面复用组件
├── layouts/
│   ├── MainLayout.vue   # 一级布局壳：侧边栏 + 顶栏 + <router-view>
│   └── RouteGroup.vue   # 二级分类容器：<router-view> 子路由出口
├── router/index.js
├── stores/user.js       # Pinia 登录态
└── utils/request.js     # axios 统一封装
```

端口约定：每个前端工程固定独立端口（如管理端 5174、用户端 5173），互不冲突。

## 二、axios 统一封装（request.js）

```js
import axios from 'axios'
import { useUserStore } from '../stores/user'

// 参数二次编码：兼容反向代理对 query string 预解码一次的行为
// （配合后端容错解码过滤器，直连/代理行为一致）
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  paramsSerializer: {
    serialize: (params) => {
      const parts = []
      Object.keys(params || {}).forEach((key) => {
        const value = params[key]
        if (value === undefined || value === null || value === '') return
        parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value)).replace(/%/g, '%25')}`)
      })
      return parts.join('&')
    }
  }
})

// 请求拦截：附加 token（键名按端隔离，如 admin_token / user_token）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：解包 {code, message, data}；401 自动登出
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body.code === 200) return body.data
    if (body.code === 401) { useUserStore().logout() }
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (err) => Promise.reject(err)
)

export default request
```

## 三、路由三级标准（核心模板）

### 3.1 RouteGroup.vue（二级分类容器）

```vue
<template>
  <router-view />
</template>
```

### 3.2 router/index.js（三级路由完整写法）

```js
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/LoginView.vue') },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),   // 一级：布局壳
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/DashboardView.vue') },

      // ===== 二级分类 + 三级功能页（标准结构，分类名按业务定）=====
      {
        path: 'admin/user',                                          // 二级：功能分类
        component: () => import('../layouts/RouteGroup.vue'),
        children: [
          { path: 'member',  name: 'user-member',   // 三级：功能页
            component: () => import('../views/admin/user/UserMemberView.vue') },
          { path: 'staff',  name: 'user-staff',
            component: () => import('../views/admin/user/UserStaffView.vue') },
          { path: 'manager', name: 'user-manager',
            component: () => import('../views/admin/user/ManagerAccountView.vue') },
          { path: '', redirect: { name: 'user-member' } }         // 分类默认跳第一个
        ]
      },

      // ===== 单一页面功能：可直接二级路由挂功能页 =====
      { path: 'admin/settings', name: 'settings',
        component: () => import('../views/admin/SystemSettingsView.vue') }
    ]
  }
]
```

### 3.3 MainLayout.vue 菜单（key 与路由 name 严格一致）

```js
import { TeamOutlined, SettingOutlined } from '@ant-design/icons-vue'

const menus = [
  { key: 'Dashboard', label: '工作台' },
  { key: 'product-group', label: '商品管理', icon: TeamOutlined, children: [   // 分组菜单
    { key: 'product-list', label: '商品列表' },
    { key: 'product-review', label: '商品审核' }
  ] },
  { key: 'user-group', label: '用户管理', icon: TeamOutlined, children: [
    { key: 'user-member', label: '会员管理' },
    { key: 'user-staff', label: '员工管理' },
    { key: 'user-manager', label: '管理员账号' }
  ] },
  { key: 'settings', label: '系统设置' }
]

// 白名单控制菜单可见性（未开发的入口不上线）
const developedRoutes = ['Dashboard', 'user-member', 'user-staff', 'user-manager', 'settings']
```

### 3.4 api 封装（api/ops.js）

```js
import request from '../utils/request'

export const userPage = (params) => request.get('/admin/ops/users', { params })
export const createUser = (data) => request.post('/admin/ops/users', data)
export const updateUser = (id, data) => request.put(`/admin/ops/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/admin/ops/users/${id}`)
export const resetUserPassword = (id, newPassword) =>
  request.post(`/admin/ops/users/${id}/reset-password`, { newPassword })
```

## 四、共用组件 + 薄壳视图模板（三级页面标准形态）

### 4.1 共用组件（UserList.vue，含移动端适配）

```vue
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as opsApi from '../../../api/ops'
import { useUserStore } from '../../../stores/user'

const props = defineProps({
  type: { type: Number, required: true },       // 数据域标识（按业务定义）
  title: { type: String, required: true }
})

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')

// 移动端适配：<=768px 切换卡片布局
const isMobile = ref(false)
function checkWidth() { isMobile.value = window.innerWidth <= 768 }

// 表格列：无 ID 列，只展示名称类字段
const columns = [
  { title: '登录名', dataIndex: 'username', width: 130 },
  { title: '展示名', dataIndex: 'nickname', width: 130 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 300, fixed: 'right' }
]

async function load() {
  loading.value = true
  try {
    const res = await opsApi.userPage({
      page: page.value, size: 10,
      keyword: keyword.value || undefined,   // 按名称搜索
      type: props.type
    })
    list.value = res.records
    total.value = Number(res.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

// 删除：二次确认，文案说明后果
function handleDelete(record) {
  Modal.confirm({
    title: '删除用户',
    content: `确定删除「${record.nickname || record.username}」吗？其全部关联业务数据将一并清除，不可恢复。`,
    okText: '删除', okType: 'danger', cancelText: '取消',
    onOk: async () => {
      try {
        await opsApi.deleteUser(record.id)
        message.success('已删除')
        load()
      } catch (e) { message.error(e.message) }
    }
  })
}

onMounted(() => { checkWidth(); window.addEventListener('resize', checkWidth); load() })
onUnmounted(() => window.removeEventListener('resize', checkWidth))
</script>

<template>
  <a-card :bordered="false">
    <template #title>{{ title }}</template>

    <!-- 工具栏：名称关键字搜索 -->
    <div class="toolbar">
      <a-input v-model:value="keyword" placeholder="名称/登录名" allow-clear
               class="toolbar-input" @press-enter="() => { page = 1; load() }" />
      <a-button type="primary" @click="() => { page = 1; load() }">查询</a-button>
    </div>

    <!-- 桌面端：表格 -->
    <a-table v-if="!isMobile" :columns="columns" :data-source="list" :loading="loading"
             row-key="id" :scroll="{ x: 'max-content' }"
             :pagination="{ current: page, total, pageSize: 10,
               onChange: (p) => { page = p; load() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'error'"
                   :text="record.status === 1 ? '正常' : '禁用'" />
        </template>
        <template v-else-if="column.key === 'action'">
          <!-- 当前账号保护：自己那行无操作 -->
          <span v-if="record.id === userStore.user?.id" class="muted">当前账号</span>
          <a-space v-else>
            <a-button size="small" @click="openEdit(record)">编辑</a-button>
            <a-button size="small" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 移动端：卡片列表 -->
    <div v-else class="mobile-list">
      <div v-for="record in list" :key="record.id" class="mobile-card">
        <div class="mobile-head">
          <a-avatar size="large">{{ (record.nickname || record.username || '?').slice(0, 1) }}</a-avatar>
          <div class="mobile-title">
            <div class="mobile-nickname">{{ record.nickname || record.username }}</div>
            <div class="mobile-username">{{ record.username }}</div>
          </div>
          <a-badge :status="record.status === 1 ? 'success' : 'error'"
                   :text="record.status === 1 ? '正常' : '禁用'" />
        </div>
        <!-- 移动端操作：块状按钮 -->
        <div class="mobile-actions">
          <a-button size="small" block @click="openEdit(record)">编辑</a-button>
          <a-button size="small" block danger @click="handleDelete(record)">删除</a-button>
        </div>
      </div>
    </div>

    <!-- 弹窗宽度自适应 -->
    <a-modal :width="isMobile ? '92%' : 520" ...>...</a-modal>
  </a-card>
</template>
```

### 4.2 薄壳视图（每个三级页面一个）

```vue
<!-- UserMemberView.vue -->
<template>
  <UserList :type="1" title="会员管理" />
</template>
<script setup>
import UserList from './UserList.vue'
</script>
```

## 五、移动端适配 CSS 标准

```css
.mobile-list { display: flex; flex-direction: column; gap: 12px; }
.mobile-card { border: 1px solid #f0f0f0; border-radius: 10px; padding: 12px; background: #fff; }
.mobile-head { display: flex; align-items: center; gap: 10px; }
.mobile-actions { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 12px; }
.mobile-actions :deep(.ant-btn) { padding-inline: 4px; font-size: 12px; }

@media (max-width: 768px) {
  .toolbar-input { flex: 1; width: auto; }   /* 搜索框填满 */
}
```

## 六、Vite 配置模板

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,                                  // 本工程固定端口
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
```

## 七、UI 检查清单（提交前自查）

- [ ] 列表/标签/详情无任何数据库 ID、内部编码（一律名称，展示名优先）
- [ ] 筛选是名称文本搜索框，不是 ID 数字框
- [ ] 路由符合三级标准（分类分组 + 功能页）
- [ ] 列表页有 isMobile 双布局；弹窗/抽屉宽度自适应
- [ ] 模板中无直接 `window` 访问（用 isMobile 响应式变量）
- [ ] 删除类操作有 Modal.confirm 二次确认
- [ ] 当前账号行显示"当前账号"且无操作按钮
- [ ] 列表有 spin / empty / 分页三件套
- [ ] 成功/失败有 message 即时反馈
- [ ] 页面未直接 import axios（走 utils/request）
