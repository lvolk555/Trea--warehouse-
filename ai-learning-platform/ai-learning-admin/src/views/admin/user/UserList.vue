<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import * as opsApi from '../../../api/ops'
import { useUserStore } from '../../../stores/user'

const props = defineProps({
  /** 页面管理的角色：1学生 2教师 3管理员 */
  role: { type: Number, required: true },
  title: { type: String, required: true }
})

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref(null)

const roleText = { 1: '学生', 2: '教师', 3: '管理员' }

const columns = [
  { title: '用户名', dataIndex: 'username', width: 130 },
  { title: '昵称', dataIndex: 'nickname', width: 130 },
  { title: '状态', key: 'status', width: 90 },
  { title: '注册时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 300, fixed: 'right' }
]

// 移动端适配：<=768px 收起表格改用卡片列表
const isMobile = ref(false)
function checkWidth() {
  isMobile.value = window.innerWidth <= 768
}

async function load() {
  loading.value = true
  try {
    const res = await opsApi.userPage({
      page: page.value, size: 10,
      keyword: keyword.value || undefined,
      role: props.role, status: statusFilter.value
    })
    list.value = res.records
    total.value = Number(res.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  load()
}

// ---------- 新增/编辑 ----------
const modalVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const saving = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })
const formRules = {
  username: [{ required: true, min: 3, max: 50, message: '用户名需 3-50 位', trigger: 'blur' }],
  password: [
    { required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' },
    { validator: (_, v) => editingId.value ? Promise.resolve() : (v && v.length >= 6 ? Promise.resolve() : Promise.reject('密码至少 6 位')), trigger: 'blur' }
  ]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', password: '', nickname: '' })
  modalVisible.value = true
}

function openEdit(record) {
  editingId.value = record.id
  Object.assign(form, { username: record.username, password: '', nickname: record.nickname })
  modalVisible.value = true
}

async function handleSave() {
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    if (editingId.value) {
      await opsApi.updateUser(editingId.value, { nickname: form.nickname, role: props.role })
      message.success('用户已更新')
    } else {
      await opsApi.createUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined,
        role: props.role
      })
      message.success('用户创建成功')
    }
    modalVisible.value = false
    load()
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

// ---------- 重置密码 ----------
const pwdVisible = ref(false)
const pwdTarget = ref(null)
const pwdSaving = ref(false)
const pwdForm = reactive({ newPassword: '', confirm: '' })

function openResetPwd(record) {
  pwdTarget.value = record
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
  pwdVisible.value = true
}

async function handleResetPwd() {
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    message.warning('新密码至少 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    message.warning('两次输入的密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    await opsApi.resetUserPassword(pwdTarget.value.id, pwdForm.newPassword)
    message.success(`已重置「${pwdTarget.value.username}」的密码`)
    pwdVisible.value = false
  } catch (e) {
    message.error(e.message)
  } finally {
    pwdSaving.value = false
  }
}

// ---------- 删除 / 启禁用 ----------
function handleDelete(record) {
  Modal.confirm({
    title: '删除用户',
    content: `确定删除用户「${record.username}」吗？其选课、学习、考试、积分等数据将一并清除，不可恢复。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await opsApi.deleteUser(record.id)
        message.success('用户已删除')
        load()
      } catch (e) {
        message.error(e.message)
      }
    }
  })
}

function handleToggleStatus(record) {
  const enable = record.status !== 1
  Modal.confirm({
    title: enable ? '启用用户' : '禁用用户',
    content: `确定${enable ? '启用' : '禁用'}用户「${record.username}」吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await opsApi.userStatus(record.id, enable)
        message.success('操作成功')
        load()
      } catch (e) {
        message.error(e.message)
      }
    }
  })
}

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

onMounted(() => {
  checkWidth()
  window.addEventListener('resize', checkWidth)
  load()
})
onUnmounted(() => window.removeEventListener('resize', checkWidth))
</script>

<template>
  <a-card :bordered="false">
    <template #title>{{ title }}</template>
    <template #extra>
      <a-button type="primary" @click="openCreate"><PlusOutlined /> 新增{{ roleText[role] }}</a-button>
    </template>

    <div class="toolbar">
      <a-input
        v-model:value="keyword" placeholder="用户名/昵称" allow-clear class="toolbar-input"
        @press-enter="handleSearch"
      />
      <a-select
        v-model:value="statusFilter" placeholder="状态" allow-clear class="toolbar-select"
        @change="handleSearch"
      >
        <a-select-option :value="1">正常</a-select-option>
        <a-select-option :value="0">禁用</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleSearch"><SearchOutlined /> 查询</a-button>
    </div>

    <!-- 桌面端：表格 -->
    <a-table
      v-if="!isMobile"
      :columns="columns" :data-source="list" :loading="loading" row-key="id"
      :scroll="{ x: 'max-content' }"
      :pagination="{ current: page, total, pageSize: 10, showTotal: t => `共 ${t} 个${roleText[role]}`, onChange: (p) => { page = p; load() } }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '正常' : '禁用'" />
        </template>
        <template v-else-if="column.key === 'action'">
          <template v-if="record.id === userStore.user?.id">
            <span class="muted">当前账号</span>
          </template>
          <a-space v-else>
            <a-button size="small" @click="openEdit(record)">编辑</a-button>
            <a-button size="small" @click="openResetPwd(record)">重置密码</a-button>
            <a-button size="small" :danger="record.status === 1" @click="handleToggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button size="small" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 移动端：卡片列表 -->
    <div v-else>
      <a-spin :spinning="loading">
        <a-empty v-if="list.length === 0" description="暂无数据" style="margin: 40px 0" />
        <div class="mobile-list">
          <div v-for="record in list" :key="record.id" class="mobile-card">
            <div class="mobile-head">
              <a-avatar size="large">{{ (record.nickname || record.username || '?').slice(0, 1) }}</a-avatar>
              <div class="mobile-title">
                <div class="mobile-nickname">{{ record.nickname || record.username }}</div>
                <div class="mobile-username">{{ record.username }}</div>
              </div>
              <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '正常' : '禁用'" />
            </div>
            <div class="mobile-meta">注册时间：{{ formatTime(record.createTime) }}</div>
            <div v-if="record.id === userStore.user?.id" class="muted mobile-self">当前账号</div>
            <div v-else class="mobile-actions">
              <a-button size="small" block @click="openEdit(record)">编辑</a-button>
              <a-button size="small" block @click="openResetPwd(record)">重置密码</a-button>
              <a-button size="small" block :danger="record.status === 1" @click="handleToggleStatus(record)">
                {{ record.status === 1 ? '禁用' : '启用' }}
              </a-button>
              <a-button size="small" block danger @click="handleDelete(record)">删除</a-button>
            </div>
          </div>
        </div>
        <div class="mobile-pager">
          <a-pagination
            :current="page" :total="total" :page-size="10" size="small"
            :show-total="t => `共 ${t} 个${roleText[role]}`"
            @change="(p) => { page = p; load() }"
          />
        </div>
      </a-spin>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible" :title="editingId ? `编辑${roleText[role]}` : `新增${roleText[role]}`"
      :confirm-loading="saving" ok-text="保存" cancel-text="取消"
      :width="isMobile ? '92%' : 520"
      @ok="handleSave"
    >
      <a-form ref="formRef" :model="form" :rules="formRules" layout="vertical" style="margin-top: 12px">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" :disabled="!!editingId" placeholder="3-50 位，创建后不可修改" />
        </a-form-item>
        <a-form-item v-if="!editingId" label="初始密码" name="password">
          <a-input-password v-model:value="form.password" placeholder="至少 6 位" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="form.nickname" placeholder="选填，默认同用户名" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置密码弹窗 -->
    <a-modal
      v-model:open="pwdVisible" :title="`重置密码 - ${pwdTarget?.username || ''}`"
      :confirm-loading="pwdSaving" ok-text="重置" cancel-text="取消"
      :width="isMobile ? '92%' : 520"
      @ok="handleResetPwd"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="pwdForm.newPassword" placeholder="至少 6 位" />
        </a-form-item>
        <a-form-item label="确认新密码" required>
          <a-input-password v-model:value="pwdForm.confirm" placeholder="再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.toolbar-input {
  width: 180px;
}
.toolbar-select {
  width: 100px;
}
.muted {
  color: #9ca3af;
  font-size: 12px;
}

/* 移动端卡片列表 */
.mobile-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mobile-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 12px;
  background: #fff;
}
.mobile-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mobile-title {
  flex: 1;
  min-width: 0;
}
.mobile-nickname {
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mobile-username {
  font-size: 12px;
  color: #9ca3af;
}
.mobile-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}
.mobile-self {
  margin-top: 8px;
  text-align: center;
}
.mobile-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 12px;
}
.mobile-actions :deep(.ant-btn) {
  padding-inline: 4px;
  font-size: 12px;
}
.mobile-pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .toolbar-input {
    flex: 1;
    width: auto;
  }
  .toolbar-select {
    width: 88px;
  }
}
</style>
