<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, SendOutlined, EditOutlined } from '@ant-design/icons-vue'
import * as courseApi from '../../api/course'
import CourseEditDrawer from '../../components/CourseEditDrawer.vue'

const loading = ref(false)
const courses = ref([])

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
  { title: '封面', key: 'cover', width: 96 },
  { title: '课程名称', dataIndex: 'title' },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '定价', key: 'price', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 260 }
]

async function loadCourses() {
  loading.value = true
  try {
    courses.value = await courseApi.teacherCourseList()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
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

async function handleSubmit(record) {
  try {
    await courseApi.submitCourse(record.id)
    message.success('已提交审核')
    loadCourses()
  } catch (e) {
    message.error(e.message)
  }
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
        await courseApi.deleteCourse(record.id)
        message.success('删除成功')
        loadCourses()
      } catch (e) {
        message.error(e.message)
      }
    }
  })
}

onMounted(loadCourses)
</script>

<template>
  <a-card title="课程管理" :bordered="false">
    <template #extra>
      <a-button type="primary" @click="openCreate"><PlusOutlined /> 新建课程</a-button>
    </template>

    <a-table :columns="columns" :data-source="courses" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 'max-content' }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="64" :height="40" class="cover-thumb" />
          <span v-else class="muted">无封面</span>
        </template>
        <template v-else-if="column.key === 'price'">
          <a-tag v-if="record.priceType === 1" color="blue">免费</a-tag>
          <a-tag v-else color="gold">{{ record.pointsPrice }} 积分</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tooltip v-if="record.status === 3" title="课程被驳回，请重新修改后保存提交">
            <a-tag color="red">已驳回</a-tag>
          </a-tooltip>
          <a-tag v-else :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <!-- 被驳回：仅允许重新修改（保存即重新提交审核），不可直接提交审核 -->
            <a-button
              v-if="record.status === 3"
              size="small"
              type="primary"
              danger
              @click="openEdit(record)"
            ><EditOutlined /> 重新修改</a-button>
            <a-button v-else size="small" @click="openEdit(record)"><EditOutlined /> 编辑</a-button>
            <!-- 仅已下架（曾审核通过）的课程可直接重新提交审核 -->
            <a-button v-if="record.status === 2" size="small" type="primary" ghost @click="handleSubmit(record)">
              <SendOutlined /> 提交审核
            </a-button>
            <a-button v-if="record.status !== 1" size="small" danger @click="handleDelete(record)">
              <DeleteOutlined />
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <CourseEditDrawer
      v-model:open="drawerOpen"
      :course-id="editingId"
      :save="courseApi.saveCourse"
      @saved="loadCourses"
    />
  </a-card>
</template>

<style scoped>
.cover-thumb {
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}
.muted {
  color: #9ca3af;
  font-size: 12px;
}
</style>