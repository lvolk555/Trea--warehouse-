<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import * as opsApi from '../../api/ops'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const statusFilter = ref(null)

const statusText = { 0: '待审核', 1: '已展示', 2: '已隐藏' }
const statusColor = { 0: 'orange', 1: 'green', 2: 'default' }

const columns = [
  { title: '评论用户', dataIndex: 'nickname', width: 120 },
  { title: '所属课程', dataIndex: 'courseTitle', width: 180 },
  { title: '评论内容', dataIndex: 'content' },
  { title: '状态', key: 'status', width: 90 },
  { title: '时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 220 }
]

async function load() {
  loading.value = true
  try {
    const res = await opsApi.commentPage({ page: page.value, size: 10, status: statusFilter.value })
    list.value = res.records
    total.value = Number(res.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleReview(record, visible) {
  try {
    await opsApi.reviewComment(record.id, visible)
    message.success(visible ? '已展示' : '已隐藏')
    load()
  } catch (e) {
    message.error(e.message)
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '删除评论',
    content: '确定删除该评论吗？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await opsApi.deleteComment(record.id)
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
  <a-card title="评论管理" :bordered="false">
    <a-alert message="学生发表的评论默认为待审核状态，审核通过后才会在课程详情页展示；可对广告/违规评论隐藏或删除。" type="info" show-icon style="margin-bottom: 16px" />
    <div class="toolbar">
      <a-space>
        <a-select v-model:value="statusFilter" placeholder="状态筛选" allow-clear style="width: 140px" @change="() => { page = 1; load() }">
          <a-select-option :value="0">待审核</a-select-option>
          <a-select-option :value="1">已展示</a-select-option>
          <a-select-option :value="2">已隐藏</a-select-option>
        </a-select>
        <a-button @click="load">刷新</a-button>
      </a-space>
    </div>

    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id"
             :scroll="{ x: 'max-content' }"
             :pagination="{ current: page, total, pageSize: 10, onChange: (p) => { page = p; load() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor[record.status]">{{ statusText[record.status] }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button v-if="record.status !== 1" size="small" type="primary" @click="handleReview(record, true)">展示</a-button>
            <a-button v-if="record.status !== 2" size="small" @click="handleReview(record, false)">隐藏</a-button>
            <a-button size="small" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 16px;
}
</style>
