<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as opsApi from '../../api/ops'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const statusFilter = ref(null)

// 编辑弹窗
const modalVisible = ref(false)
const form = ref({ title: '', content: '', type: 1, top: 0 })
const editingId = ref(null)

const typeText = { 1: '系统通知', 2: '活动公告', 3: '课程上新' }

const columns = [
  { title: '标题', dataIndex: 'title' },
  { title: '类型', key: 'type', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '置顶', key: 'top', width: 80 },
  { title: '发布时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 260 }
]

async function load() {
  loading.value = true
  try {
    const res = await opsApi.noticePage({ page: page.value, size: 10, status: statusFilter.value })
    list.value = res.records
    total.value = Number(res.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { title: '', content: '', type: 1, top: 0 }
  modalVisible.value = true
}

function openEdit(record) {
  editingId.value = record.id
  form.value = { title: record.title, content: record.content, type: record.type, top: record.top }
  modalVisible.value = true
}

async function handleSave() {
  if (!form.value.title.trim()) {
    message.warning('标题不能为空')
    return
  }
  try {
    await opsApi.saveNotice({ id: editingId.value, ...form.value })
    message.success(editingId.value ? '已更新' : '已发布')
    modalVisible.value = false
    load()
  } catch (e) {
    message.error(e.message)
  }
}

async function handleStatus(record) {
  try {
    await opsApi.noticeStatus(record.id, record.status !== 1)
    message.success(record.status === 1 ? '已撤回' : '已发布')
    load()
  } catch (e) {
    message.error(e.message)
  }
}

async function handleTop(record) {
  try {
    await opsApi.noticeTop(record.id, record.top !== 1)
    load()
  } catch (e) {
    message.error(e.message)
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '删除公告',
    content: `确定删除公告「${record.title}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await opsApi.deleteNotice(record.id)
        message.success('已删除')
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
  <a-card title="公告管理" :bordered="false">
    <div class="toolbar">
      <a-space>
        <a-select v-model:value="statusFilter" placeholder="状态筛选" allow-clear style="width: 140px" @change="() => { page = 1; load() }">
          <a-select-option :value="1">已发布</a-select-option>
          <a-select-option :value="0">已撤回</a-select-option>
        </a-select>
        <a-button @click="load">刷新</a-button>
      </a-space>
      <a-button type="primary" @click="openCreate">发布公告</a-button>
    </div>

    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id"
             :pagination="{ current: page, total, pageSize: 10, onChange: (p) => { page = p; load() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag>{{ typeText[record.type] || '公告' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '已发布' : '已撤回' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'top'">
          <a-tag v-if="record.top === 1" color="red">置顶</a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openEdit(record)">编辑</a-button>
            <a-button size="small" @click="handleTop(record)">{{ record.top === 1 ? '取消置顶' : '置顶' }}</a-button>
            <a-button size="small" @click="handleStatus(record)">{{ record.status === 1 ? '撤回' : '发布' }}</a-button>
            <a-button size="small" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑公告' : '发布公告'" @ok="handleSave" ok-text="保存" cancel-text="取消">
      <a-form layout="vertical">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" placeholder="公告标题" />
        </a-form-item>
        <a-form-item label="类型">
          <a-radio-group v-model:value="form.type">
            <a-radio :value="1">系统通知</a-radio>
            <a-radio :value="2">活动公告</a-radio>
            <a-radio :value="3">课程上新</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="内容">
          <a-textarea v-model:value="form.content" :rows="5" placeholder="公告内容" />
        </a-form-item>
        <a-form-item label="置顶">
          <a-switch :checked="form.top === 1" @change="(v) => form.top = v ? 1 : 0" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
</style>
