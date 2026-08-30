<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import * as opsApi from '../../api/ops'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const userIdFilter = ref(null)

const columns = [
  { title: '学生 ID', dataIndex: 'userId', width: 100 },
  { title: '课程 ID', dataIndex: 'courseId', width: 100 },
  { title: '消耗积分', dataIndex: 'pointsCost', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '兑换时间', dataIndex: 'createTime' }
]

async function load() {
  loading.value = true
  try {
    const res = await opsApi.exchangeRecords({ page: page.value, size: 10, userId: userIdFilter.value })
    list.value = res.records
    total.value = Number(res.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <a-card title="兑换记录" :bordered="false">
    <div class="toolbar">
      <a-space>
        <a-input-number v-model:value="userIdFilter" placeholder="按学生 ID 筛选" :min="1" style="width: 160px" />
        <a-button type="primary" @click="() => { page = 1; load() }">查询</a-button>
        <a-button @click="() => { userIdFilter = null; page = 1; load() }">重置</a-button>
      </a-space>
    </div>
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id"
             :pagination="{ current: page, total, pageSize: 10, onChange: (p) => { page = p; load() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '成功' : '失败' }}</a-tag>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
