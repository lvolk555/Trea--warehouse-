<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, SendOutlined, EditOutlined } from '@ant-design/icons-vue'
import * as courseApi from '../../api/course'

const loading = ref(false)
const courses = ref([])

// 状态映射
const statusMap = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '已上架', color: 'green' },
  2: { text: '已下架', color: 'default' }
}

// 编辑抽屉
const drawerVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  title: '',
  cover: '',
  category: '编程',
  description: '',
  priceType: 1,
  pointsPrice: 0,
  chapters: []
})

const columns = [
  { title: '课程名称', dataIndex: 'title' },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '定价', key: 'price', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 220 }
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
  Object.assign(form, {
    id: null, title: '', cover: '', category: '编程', description: '',
    priceType: 1, pointsPrice: 0, chapters: [{ title: '', sortOrder: 1, videos: [] }]
  })
  drawerVisible.value = true
}

async function openEdit(record) {
  // 拉取课程详情（含章节视频树）填充表单
  try {
    const detail = await courseApi.courseDetail(record.id)
    Object.assign(form, {
      id: detail.id,
      title: detail.title,
      cover: detail.cover || '',
      category: detail.category || '编程',
      description: detail.description || '',
      priceType: detail.priceType,
      pointsPrice: detail.pointsPrice || 0,
      chapters: (detail.chapters || []).map(c => ({
        id: c.id,
        title: c.title,
        sortOrder: c.sortOrder,
        videos: (c.videos || []).map(v => ({
          id: v.id, title: v.title, url: v.url, duration: v.duration, sortOrder: v.sortOrder
        }))
      }))
    })
    drawerVisible.value = true
  } catch (e) {
    message.error(e.message)
  }
}

function addChapter() {
  form.chapters.push({ title: '', sortOrder: form.chapters.length + 1, videos: [] })
}

function removeChapter(index) {
  form.chapters.splice(index, 1)
}

function addVideo(chapter) {
  chapter.videos.push({ title: '', url: '', duration: 0, sortOrder: chapter.videos.length + 1 })
}

function removeVideo(chapter, index) {
  chapter.videos.splice(index, 1)
}

async function handleSave() {
  if (!form.title) {
    message.warning('请填写课程名称')
    return
  }
  if (form.priceType === 2 && (!form.pointsPrice || form.pointsPrice <= 0)) {
    message.warning('积分兑换课程需填写所需积分')
    return
  }
  saving.value = true
  try {
    await courseApi.saveCourse({ ...form })
    message.success('保存成功，课程已进入待审核状态')
    drawerVisible.value = false
    loadCourses()
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
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

    <a-table :columns="columns" :data-source="courses" :loading="loading" row-key="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'price'">
          <a-tag v-if="record.priceType === 1" color="blue">免费</a-tag>
          <a-tag v-else color="gold">{{ record.pointsPrice }} 积分</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openEdit(record)"><EditOutlined /> 编辑</a-button>
            <a-button v-if="record.status !== 0" size="small" type="primary" ghost @click="handleSubmit(record)">
              <SendOutlined /> 提交审核
            </a-button>
            <a-button v-if="record.status !== 1" size="small" danger @click="handleDelete(record)">
              <DeleteOutlined />
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 课程编辑抽屉：基本信息 + 章节视频结构 -->
    <a-drawer v-model:open="drawerVisible" :title="form.id ? '编辑课程' : '新建课程'" width="720" :closable="true">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="课程名称" required>
              <a-input v-model:value="form.title" placeholder="如：Java 面向对象程序设计" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="分类">
              <a-select v-model:value="form.category" :options="[
                { value: '编程' }, { value: '数学' }, { value: '外语' }, { value: '设计' }, { value: '其他' }
              ]" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="封面图片 URL">
          <a-input v-model:value="form.cover" placeholder="https://..." />
        </a-form-item>
        <a-form-item label="课程简介（将作为 AI 答疑的课程上下文）">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="介绍课程内容与目标" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="定价方式">
              <a-radio-group v-model:value="form.priceType">
                <a-radio :value="1">免费</a-radio>
                <a-radio :value="2">积分兑换</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="form.priceType === 2">
            <a-form-item label="所需积分">
              <a-input-number v-model:value="form.pointsPrice" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider>章节与视频</a-divider>
        <div v-for="(chapter, ci) in form.chapters" :key="ci" class="chapter-block">
          <div class="chapter-head">
            <a-input v-model:value="chapter.title" :placeholder="`第 ${ci + 1} 章标题`" style="flex: 1" />
            <a-button danger size="small" @click="removeChapter(ci)"><DeleteOutlined /> 删除章节</a-button>
          </div>
          <div v-for="(video, vi) in chapter.videos" :key="vi" class="video-row">
            <a-input v-model:value="video.title" placeholder="视频标题" style="flex: 2" />
            <a-input v-model:value="video.url" placeholder="视频地址 URL" style="flex: 3" />
            <a-input-number v-model:value="video.duration" :min="0" placeholder="时长(秒)" style="width: 110px" />
            <a-button danger size="small" shape="circle" @click="removeVideo(chapter, vi)"><DeleteOutlined /></a-button>
          </div>
          <a-button size="small" @click="addVideo(chapter)"><PlusOutlined /> 添加视频</a-button>
        </div>
        <a-button block @click="addChapter"><PlusOutlined /> 添加章节</a-button>
      </a-form>

      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">保存（进入待审核）</a-button>
        </a-space>
      </template>
    </a-drawer>
  </a-card>
</template>

<style scoped>
.chapter-block {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  background: #fafafa;
}
.chapter-head {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.video-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>
