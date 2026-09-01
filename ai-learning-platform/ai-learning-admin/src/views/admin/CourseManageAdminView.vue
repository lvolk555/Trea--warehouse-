<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import * as courseApi from '../../api/course'
import CourseEditDrawer from '../../components/CourseEditDrawer.vue'

const loading = ref(false)
const data = ref({ records: [], total: 0 })
const query = reactive({ page: 1, size: 10, status: null, keyword: '' })

const statusMap = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '已上架', color: 'green' },
  2: { text: '已下架', color: 'default' },
  3: { text: '已驳回', color: 'red' }
}

// 课程编辑抽屉
const drawerOpen = ref(false)
const editingId = ref(null)

const columns = [
  { title: '课程名称', dataIndex: 'title' },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '定价', key: 'price', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 220 }
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

function openCreate() {
  editingId.value = null
  drawerOpen.value = true
}

function openEdit(record) {
  editingId.value = record.id
  drawerOpen.value = true
}

function handleDelete(record) {
  Modal.confirm({
    title: '删除课程',
    content: `确定删除课程《${record.title}》吗？章节与视频将一并删除。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await courseApi.adminDeleteCourse(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error(e.message)
      }
    }
  })
}

function handleTableChange(pagination) {
  query.page = pagination.current
  loadData()
}

onMounted(loadData)
</script>

<template>
  <a-card title="课程管理" :bordered="false">
    <template #extra>
      <a-button type="primary" @click="openCreate"><PlusOutlined /> 新建课程</a-button>
    </template>

    <div class="toolbar">
      <a-input v-model:value="query.keyword" placeholder="搜索课程名称" style="width: 240px" allow-clear @press-enter="query.page = 1; loadData()">
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <a-select v-model:value="query.status" placeholder="状态筛选" style="width: 140px" allow-clear @change="query.page = 1; loadData()">
        <a-select-option :value="0">待审核</a-select-option>
        <a-select-option :value="1">已上架</a-select-option>
        <a-select-option :value="2">已下架</a-select-option>
        <a-select-option :value="3">已驳回</a-select-option>
      </a-select>
    </div>

    <a-table
      :columns="columns"
      :data-source="data.records"
      :loading="loading"
      row-key="id"
      :scroll="{ x: 'max-content' }"
      :pagination="{ current: query.page, pageSize: query.size, total: data.total, showTotal: t => `共 ${t} 门课程` }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'price'">
          <a-tag v-if="record.priceType === 1" color="blue">免费</a-tag>
          <a-tag v-else color="gold">{{ record.pointsPrice }} 积分</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tooltip v-if="record.status === 3" title="已被驳回，需教师重新修改提交审核后才可上下架">
            <a-tag color="red">已驳回</a-tag>
          </a-tooltip>
          <a-tag v-else :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openEdit(record)"><EditOutlined /> 编辑</a-button>
            <a-button v-if="record.status === 1" size="small" danger @click="handleToggle(record)">下架</a-button>
            <a-button v-else-if="record.status === 2" size="small" type="primary" ghost @click="handleToggle(record)">上架</a-button>
            <a-button v-if="record.status !== 1" size="small" danger @click="handleDelete(record)"><DeleteOutlined /></a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <CourseEditDrawer
      v-model:open="drawerOpen"
      :course-id="editingId"
      :save="courseApi.adminSaveCourse"
      @saved="loadData"
    />
  </a-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
</style>