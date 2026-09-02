<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as opsApi from '../../api/ops'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const roleFilter = ref(null)
const statusFilter = ref(null)

const roleText = { 1: '学生', 2: '教师', 3: '管理员' }
const roleColor = { 1: 'blue', 2: 'purple', 3: 'red' }

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: '用户名', dataIndex: 'username', width: 130 },
  { title: '昵称', dataIndex: 'nickname', width: 130 },
  { title: '角色', key: 'role', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '注册时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 330, fixed: 'right' }
]

async function load() {
  loading.value = true
  try {
    const res = await opsApi.userPage({
      page: page.value, size: 10,
      keyword: keyword.value || undefined,
      role: roleFilter.value, status: statusFilter.value
    })
    list.value = res.records
    total.value = Number(res.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

// ---------- 新增/编辑用户 ----------
const modalVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const saving = ref(false)
const form = reactive({ username: '', password: '', nickname: '', role: 1 })
const formRules = {
  username: [{ required: true, min: 3, max: 50, message: '用户名需 3-50 位', trigger: 'blur' }],
  password: [
    { required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' },
    { validator: (_, v) => editingId.value ? Promise.resolve() : (v && v.length >= 6 ? Promise.resolve() : Promise.reject('密码至少 6 位')), trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', password: '', nickname: '', role: 1 })
  modalVisible.value = true
}

function openEdit(record) {
  editingId.value = record.id
  Object.assign(form, { username: record.username, password: '', nickname: record.nickname, role: record.role })
  modalVisible.value = true
}

async function handleSave() {
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    if (editingId.value) {
      await opsApi.updateUser(editingId.value, {
        nickname: form.nickname,
        role: form.role
      })
      message.success('用户已更新')
    } else {
      await opsApi.createUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined,
        role: form.role
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

// ---------- 删除用户 ----------
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

onMounted(load)
</script>

<template>
  <a-card title="用户管理" :bordered="false">
    <div class="toolbar">
      <a-space wrap>
        <a-input v-model:value="keyword" placeholder="用户名/昵称" allow-clear style="width: 160px" @press-enter="() => { page = 1; load() }" />
        <a-select v-model:value="roleFilter" placeholder="角色" allow-clear style="width: 110px" @change="() => { page = 1; load() }">
          <a-select-option :value="1">学生</a-select-option>
          <a-select-option :value="2">教师</a-select-option>
          <a-select-option :value="3">管理员</a-select-option>
        </a-select>
        <a-select v-model:value="statusFilter" placeholder="状态" allow-clear style="width: 110px" @change="() => { page = 1; load() }">
          <a-select-option :value="1">正常</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
        <a-button type="primary" @click="() => { page = 1; load() }">查询</a-button>
        <a-button type="primary" ghost @click="openCreate">新增用户</a-button>
      </a-space>
    </div>

    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id"
             :scroll="{ x: 'max-content' }"
             :pagination="{ current: page, total, pageSize: 10, onChange: (p) => { page = p; load() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'role'">
          <a-tag :color="roleColor[record.role]">{{ roleText[record.role] }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
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

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑用户' : '新增用户'"
             :confirm-loading="saving" ok-text="保存" cancel-text="取消" @ok="handleSave">
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
        <a-form-item label="角色" name="role">
          <a-select v-model:value="form.role">
            <a-select-option :value="1">学生</a-select-option>
            <a-select-option :value="2">教师</a-select-option>
            <a-select-option :value="3">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置密码弹窗 -->
    <a-modal v-model:open="pwdVisible" :title="`重置密码 - ${pwdTarget?.username || ''}`"
             :confirm-loading="pwdSaving" ok-text="重置" cancel-text="取消" @ok="handleResetPwd">
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
  margin-bottom: 16px;
}
.muted {
  color: #9ca3af;
  font-size: 12px;
}
</style>
