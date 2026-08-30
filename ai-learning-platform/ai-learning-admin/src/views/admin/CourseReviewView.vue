<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { CheckOutlined, CloseOutlined, EyeOutlined } from '@ant-design/icons-vue'
import * as courseApi from '../../api/course'

const loading = ref(false)
const courses = ref([])
const previewVisible = ref(false)
const previewCourse = ref(null)

const columns = [
  { title: '课程名称', dataIndex: 'title' },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '定价', key: 'price', width: 140 },
  { title: '提交时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 240 }
]

async function loadPending() {
  loading.value = true
  try {
    courses.value = await courseApi.pendingCourses()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleReview(record, approved) {
  try {
    await courseApi.reviewCourse({ courseId: record.id, approved })
    message.success(approved ? '已通过，课程已上架' : '已驳回')
    loadPending()
  } catch (e) {
    message.error(e.message)
  }
}

async function handlePreview(record) {
  try {
    previewCourse.value = await courseApi.courseDetail(record.id)
    previewVisible.value = true
  } catch (e) {
    message.error(e.message)
  }
}

onMounted(loadPending)
</script>

<template>
  <a-card title="课程审核" :bordered="false">
    <a-alert message="教师提交/修改课程后进入待审核状态，审核通过后课程上架到学生端课程广场。" type="info" show-icon style="margin-bottom: 16px" />
    <a-table :columns="columns" :data-source="courses" :loading="loading" row-key="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'price'">
          <a-tag v-if="record.priceType === 1" color="blue">免费</a-tag>
          <a-tag v-else color="gold">{{ record.pointsPrice }} 积分</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="handlePreview(record)"><EyeOutlined /> 预览</a-button>
            <a-button size="small" type="primary" @click="handleReview(record, true)"><CheckOutlined /> 通过</a-button>
            <a-button size="small" danger @click="handleReview(record, false)"><CloseOutlined /> 驳回</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 课程预览 -->
    <a-modal v-model:open="previewVisible" :title="`课程预览：${previewCourse?.title || ''}`" width="640px" :footer="null">
      <template v-if="previewCourse">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="分类">{{ previewCourse.category }}</a-descriptions-item>
          <a-descriptions-item label="定价">
            {{ previewCourse.priceType === 1 ? '免费' : `${previewCourse.pointsPrice} 积分` }}
          </a-descriptions-item>
          <a-descriptions-item label="课程简介" :span="2">{{ previewCourse.description }}</a-descriptions-item>
        </a-descriptions>
        <a-divider>章节结构</a-divider>
        <a-collapse>
          <a-collapse-panel v-for="c in previewCourse.chapters" :key="c.id" :header="c.title">
            <div v-for="v in c.videos" :key="v.id" class="video-item">
              {{ v.title }}
              <span class="duration">{{ Math.round((v.duration || 0) / 60) }} 分钟</span>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </template>
    </a-modal>
  </a-card>
</template>

<style scoped>
.video-item {
  padding: 4px 0;
  display: flex;
  justify-content: space-between;
}
.duration {
  color: #9ca3af;
  font-size: 12px;
}
</style>
