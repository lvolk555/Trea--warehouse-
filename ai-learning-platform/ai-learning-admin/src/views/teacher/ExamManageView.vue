<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, EditOutlined, SendOutlined } from '@ant-design/icons-vue'
import * as questionApi from '../../api/question'
import * as courseApi from '../../api/course'

const typeMap = {
  1: { text: '单选', color: 'blue' },
  2: { text: '多选', color: 'purple' },
  3: { text: '判断', color: 'cyan' },
  4: { text: '简答', color: 'orange' }
}

const loading = ref(false)
const exams = ref([])
const courses = ref([])

const columns = [
  { title: '试卷名称', dataIndex: 'title' },
  { title: '所属课程', dataIndex: 'courseTitle', width: 160 },
  { title: '题目数', key: 'count', width: 80 },
  { title: '时长(分钟)', dataIndex: 'duration', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 200 }
]

// 组卷抽屉
const drawerVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  courseId: null,
  title: '',
  duration: 60,
  questionIds: []
})

// 当前课程下的候选题目（用于勾选出题）
const candidateQuestions = ref([])
const loadingCandidates = ref(false)

const formCourseTitle = computed(() =>
  courses.value.find(c => c.id === form.courseId)?.title || ''
)

async function loadCourses() {
  try {
    courses.value = await courseApi.teacherCourseList()
  } catch (e) {
    message.error(e.message)
  }
}

async function loadExams() {
  loading.value = true
  try {
    exams.value = await questionApi.teacherExamList()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadCandidates(courseId) {
  if (!courseId) {
    candidateQuestions.value = []
    return
  }
  loadingCandidates.value = true
  try {
    const page = await questionApi.questionPage({ pageNum: 1, pageSize: 200, courseId })
    candidateQuestions.value = page.records
  } catch (e) {
    message.error(e.message)
  } finally {
    loadingCandidates.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, courseId: null, title: '', duration: 60, questionIds: [] })
  candidateQuestions.value = []
  drawerVisible.value = true
}

function openEdit(record) {
  Object.assign(form, {
    id: record.id,
    courseId: record.courseId,
    title: record.title,
    duration: record.duration,
    questionIds: record.questionIds || []
  })
  loadCandidates(record.courseId)
  drawerVisible.value = true
}

function onFormCourseChange(courseId) {
  form.questionIds = []
  loadCandidates(courseId)
}

async function handleSave(publish) {
  if (!form.courseId) return message.warning('请选择所属课程')
  if (!form.title.trim()) return message.warning('请输入试卷名称')
  if (form.questionIds.length === 0) return message.warning('请至少勾选一道题目')

  saving.value = true
  try {
    await questionApi.saveExam({
      id: form.id,
      courseId: form.courseId,
      title: form.title,
      duration: form.duration,
      questionIds: form.questionIds,
      status: publish ? 1 : 0
    })
    message.success(publish ? '已保存并发布' : '已保存为草稿')
    drawerVisible.value = false
    loadExams()
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

async function handlePublish(record) {
  try {
    await questionApi.publishExam(record.id)
    message.success('试卷已发布，学生可参加考试')
    loadExams()
  } catch (e) {
    message.error(e.message)
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '删除试卷',
    content: '确定删除该试卷吗？相关考试记录不受影响。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await questionApi.deleteExam(record.id)
      message.success('已删除')
      loadExams()
    }
  })
}

onMounted(() => {
  loadCourses()
  loadExams()
})
</script>

<template>
  <div>
    <a-card :bordered="false">
      <template #title>
        <a-space>
          <span>试卷列表</span>
        </a-space>
      </template>
      <template #extra>
        <a-button type="primary" @click="openCreate"><PlusOutlined /> 新建试卷</a-button>
      </template>

      <a-table :columns="columns" :data-source="exams" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 'max-content' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'count'">
            {{ record.questionIds?.length || 0 }} 题
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '已发布' : '草稿' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a v-if="record.status === 0" @click="handlePublish(record)">
                <SendOutlined /> 发布
              </a>
              <a style="color: #ff4d4f" @click="handleDelete(record)"><DeleteOutlined /> 删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 组卷抽屉 -->
    <a-drawer v-model:open="drawerVisible" :title="form.id ? '编辑试卷' : '新建试卷'" width="720"
      :body-style="{ paddingBottom: '80px' }">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="所属课程" required>
              <a-select v-model:value="form.courseId" placeholder="选择课程" @change="onFormCourseChange">
                <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.title }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="考试时长（分钟）" required>
              <a-input-number v-model:value="form.duration" :min="5" :max="240" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="试卷名称" required>
          <a-input v-model:value="form.title" placeholder="如：Java 基础第一章测验" />
        </a-form-item>

        <a-divider>选择题目（已选 {{ form.questionIds.length }} 题）</a-divider>

        <a-spin :spinning="loadingCandidates">
          <a-empty v-if="!form.courseId" description="请先选择课程" />
          <a-empty v-else-if="candidateQuestions.length === 0"
            description="该课程暂无题目，请先到题库管理录入" />
          <a-checkbox-group v-else v-model:value="form.questionIds" style="width: 100%">
            <div v-for="q in candidateQuestions" :key="q.id" class="question-item">
              <a-checkbox :value="q.id">
                <a-tag :color="typeMap[q.type]?.color" style="margin-right: 8px">
                  {{ typeMap[q.type]?.text }}
                </a-tag>
                <span class="q-content">{{ q.content }}</span>
                <span class="q-chapter">{{ q.chapterTitle }}</span>
              </a-checkbox>
            </div>
          </a-checkbox-group>
        </a-spin>
      </a-form>

      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button :loading="saving" @click="handleSave(false)">保存草稿</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave(true)">保存并发布</a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<style scoped>
.question-item {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}
.question-item:hover {
  background: #fafafa;
}
.q-content {
  margin-right: 8px;
}
.q-chapter {
  color: #999;
  font-size: 12px;
}
</style>
