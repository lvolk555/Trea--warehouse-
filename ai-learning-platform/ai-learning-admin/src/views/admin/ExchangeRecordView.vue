<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import * as opsApi from '../../api/ops'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')

const columns = [
  { title: '学生名称', dataIndex: 'studentName', width: 140 },
  { title: '课程名称', dataIndex: 'courseName', width: 220 },
  { title: '抵扣积分', key: 'discount', width: 100 },
  { title: '消耗积分', dataIndex: 'pointsCost', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '兑换时间', dataIndex: 'createTime' }
]

async function load() {
  loading.value = true
  try {
    const res = await opsApi.exchangeRecords({
      page: page.value, size: 10,
      keyword: keyword.value || undefined
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

onMounted(load)
</script>

<template>
  <a-card title="兑换记录" :bordered="false">
    <div class="toolbar">
      <a-space wrap>
        <a-input
          v-model:value="keyword" placeholder="按学生名称/用户名搜索" allow-clear style="width: 200px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="() => { keyword = ''; page = 1; load() }">重置</a-button>
      </a-space>
    </div>
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id"
             :scroll="{ x: 'max-content' }"
             :pagination="{ current: page, total, pageSize: 10, onChange: (p) => { page = p; load() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '成功' : '失败' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'discount'">
          <span>{{ record.discount ? `-${record.discount}` : '-' }}</span>
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
