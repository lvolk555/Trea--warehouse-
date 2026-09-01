<script setup>
import { ref, onMounted } from 'vue'
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
  { title: '操作', key: 'action', width: 240 }
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

async function handleChangeRole(record, role) {
  try {
    await opsApi.userRole(record.id, role)
    message.success('角色已调整')
    load()
  } catch (e) {
    message.error(e.message)
  }
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
            <a-select :value="record.role" size="small" style="width: 90px" @change="(v) => handleChangeRole(record, v)">
              <a-select-option :value="1">学生</a-select-option>
              <a-select-option :value="2">教师</a-select-option>
              <a-select-option :value="3">管理员</a-select-option>
            </a-select>
            <a-button size="small" :danger="record.status === 1" @click="handleToggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
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
