<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import * as courseApi from '../../api/course'

const loading = ref(false)
const data = ref({ records: [], total: 0 })
const query = reactive({ page: 1, size: 10, status: null, keyword: '' })

const statusMap = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '已上架', color: 'green' },
  2: { text: '已下架', color: 'default' }
}

const columns = [
  { title: '课程名称', dataIndex: 'title' },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '定价', key: 'price', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 140 }
]

async function loadData() {
  loading.value = true
  try {
    data.value = await courseApi.adminCoursePage(query)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleToggle(record) {
  const online = record.status !== 1
  try {
    await courseApi.changeCourseStatus(record.id, online)
    message.success(online ? '已上架' : '已下架')
    loadData()
  } catch (e) {
    message.error(e.message)
  }
}

function handleTableChange(pagination) {
  query.page = pagination.current
  loadData()
}

onMounted(loadData)
</script>

<template>
  <a-card title="课程管理" :bordered="false">
    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="搜索课程名称" style="width: 240px" allow-clear @press-enter="query.page = 1; loadData()">
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <a-select v-model:value="query.status" placeholder="状态筛选" style="width: 140px" allow-clear @change="query.page = 1; loadData()">
        <a-select-option :value="0">待审核</a-select-option>
        <a-select-option :value="1">已上架</a-select-option>
        <a-select-option :value="2">已下架</a-select-option>
      </a-select>
    </div>

    <a-table
      :columns="columns"
      :data-source="data.records"
      :loading="loading"
      row-key="id"
      :pagination="{ current: query.page, pageSize: query.size, total: data.total, showTotal: t => `共 ${t} 门课程` }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'price'">
          <a-tag v-if="record.priceType === 1" color="blue">免费</a-tag>
          <a-tag v-else color="gold">{{ record.pointsPrice }} 积分</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button v-if="record.status === 1" size="small" danger @click="handleToggle(record)">下架</a-button>
          <a-button v-else size="small" type="primary" ghost @click="handleToggle(record)">上架</a-button>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
