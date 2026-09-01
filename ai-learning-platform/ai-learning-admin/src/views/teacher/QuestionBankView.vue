<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'
import * as questionApi from '../../api/question'
import * as courseApi from '../../api/course'

// 题型映射
const typeMap = {
  1: { text: '单选题', color: 'blue' },
  2: { text: '多选题', color: 'purple' },
  3: { text: '判断题', color: 'cyan' },
  4: { text: '简答题', color: 'orange' }
}

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, courseId: null, chapterId: null, type: null })

// 课程/章节级联数据
const courses = ref([])
const chapters = ref([])

const columns = [
  { title: '题干', dataIndex: 'content', ellipsis: true },
  { title: '所属课程', dataIndex: 'courseTitle', width: 160 },
  { title: '所属章节', dataIndex: 'chapterTitle', width: 140 },
  { title: '题型', key: 'type', width: 90 },
  { title: '答案', dataIndex: 'answer', width: 100, ellipsis: true },
  { title: '来源', key: 'source', width: 90 },
  { title: '操作', key: 'action', width: 130 }
]

// 编辑弹窗
const modalVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  courseId: null,
  chapterId: null,
  type: 1,
  content: '',
  options: ['', '', '', ''],
  answer: '',
  multiAnswers: [],
  judgeAnswer: '对',
  analysis: ''
})

const formChapters = computed(() => {
  const course = courses.value.find(c => c.id === form.courseId)
  return course?.chapters || []
})

async function loadCourses() {
  try {
    const list = await courseApi.teacherCourseList()
    // 并发拉取每门课的章节结构
    const detailed = await Promise.all(
      list.map(async (c) => {
        try {
          const detail = await courseApi.courseDetail(c.id)
          return { id: c.id, title: c.title, chapters: detail.chapters || [] }
        } catch {
          return { id: c.id, title: c.title, chapters: [] }
        }
      })
    )
    courses.value = detailed
  } catch (e) {
    message.error(e.message)
  }
}

async function loadQuestions() {
  loading.value = true
  try {
    const page = await questionApi.questionPage(query)
    records.value = page.records
    total.value = Number(page.total)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function onCourseChange(courseId) {
  query.chapterId = null
  const course = courses.value.find(c => c.id === courseId)
  chapters.value = course?.chapters || []
  loadQuestions()
}

function openCreate() {
  Object.assign(form, {
    id: null, courseId: null, chapterId: null, type: 1, content: '',
    options: ['', '', '', ''], answer: '', multiAnswers: [], judgeAnswer: '对', analysis: ''
  })
  modalVisible.value = true
}

function openEdit(record) {
  Object.assign(form, {
    id: record.id,
    courseId: record.courseId,
    chapterId: record.chapterId,
    type: record.type,
    content: record.content,
    options: record.options?.length ? [...record.options] : ['', '', '', ''],
    answer: record.answer,
    multiAnswers: record.type === 2 ? record.answer.replace(/[,，、\s]/g, '').split('') : [],
    judgeAnswer: record.type === 3 ? record.answer : '对',
    analysis: record.analysis || ''
  })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.courseId || !form.chapterId) {
    return message.warning('请选择所属课程和章节')
  }
  if (!form.content.trim()) {
    return message.warning('请输入题干')
  }
  // 按题型组装答案
  let answer = form.answer
  let options = null
  if (form.type === 1) {
    answer = form.answer
    options = form.options.filter(o => o.trim())
    if (!['A', 'B', 'C', 'D', 'E', 'F'].includes(answer)) {
      return message.warning('单选题答案请填写选项字母（如 A）')
    }
  } else if (form.type === 2) {
    answer = form.multiAnswers.join('')
    options = form.options.filter(o => o.trim())
    if (!answer) {
      return message.warning('请勾选多选题正确答案')
    }
  } else if (form.type === 3) {
    answer = form.judgeAnswer
  }
  if (!answer?.trim()) {
    return message.warning('请填写正确答案')
  }

  saving.value = true
  try {
    await questionApi.saveQuestion({
      id: form.id,
      courseId: form.courseId,
      chapterId: form.chapterId,
      type: form.type,
      content: form.content,
      options,
      answer,
      analysis: form.analysis
    })
    message.success('保存成功')
    modalVisible.value = false
    loadQuestions()
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '删除题目',
    content: '确定删除该题目吗？删除后引用该题的试卷将受影响。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await questionApi.deleteQuestion(record.id)
      message.success('已删除')
      loadQuestions()
    }
  })
}

onMounted(() => {
  loadCourses()
  loadQuestions()
})
</script>

<template>
  <div>
    <!-- 筛选栏 -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-space wrap>
        <a-select v-model:value="query.courseId" placeholder="选择课程" style="width: 200px"
          allow-clear @change="onCourseChange">
          <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.title }}</a-select-option>
        </a-select>
        <a-select v-model:value="query.chapterId" placeholder="选择章节" style="width: 180px"
          allow-clear :disabled="!query.courseId" @change="loadQuestions">
          <a-select-option v-for="ch in chapters" :key="ch.id" :value="ch.id">{{ ch.title }}</a-select-option>
        </a-select>
        <a-select v-model:value="query.type" placeholder="题型" style="width: 120px"
          allow-clear @change="loadQuestions">
          <a-select-option v-for="(v, k) in typeMap" :key="k" :value="Number(k)">{{ v.text }}</a-select-option>
        </a-select>
        <a-button type="primary" @click="openCreate">
          <PlusOutlined /> 新增题目
        </a-button>
      </a-space>
    </a-card>

    <!-- 题目列表 -->
    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="records" :loading="loading" row-key="id"
        :scroll="{ x: 'max-content' }"
        :pagination="{
          current: query.pageNum, pageSize: query.pageSize, total, showSizeChanger: true,
          onChange: (p, s) => { query.pageNum = p; query.pageSize = s; loadQuestions() }
        }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="typeMap[record.type]?.color">{{ typeMap[record.type]?.text }}</a-tag>
          </template>
          <template v-else-if="column.key === 'source'">
            <a-tag :color="record.source === 2 ? 'geekblue' : 'default'">
              {{ record.source === 2 ? 'AI 生成' : '人工录入' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openEdit(record)"><EditOutlined /> 编辑</a>
              <a style="color: #ff4d4f" @click="handleDelete(record)"><DeleteOutlined /> 删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:open="modalVisible" :title="form.id ? '编辑题目' : '新增题目'" width="640px"
      :confirm-loading="saving" ok-text="保存" cancel-text="取消" @ok="handleSave">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="所属课程" required>
              <a-select v-model:value="form.courseId" placeholder="选择课程" @change="form.chapterId = null">
                <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.title }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="所属章节" required>
              <a-select v-model:value="form.chapterId" placeholder="先选择课程" :disabled="!form.courseId">
                <a-select-option v-for="ch in formChapters" :key="ch.id" :value="ch.id">{{ ch.title }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="题型" required>
          <a-radio-group v-model:value="form.type">
            <a-radio-button v-for="(v, k) in typeMap" :key="k" :value="Number(k)">{{ v.text }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="题干" required>
          <a-textarea v-model:value="form.content" :rows="3" placeholder="请输入题干" />
        </a-form-item>

        <!-- 客观题选项 -->
        <template v-if="form.type === 1 || form.type === 2">
          <a-form-item label="选项">
            <div v-for="(opt, i) in form.options" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px">
              <a-input :value="String.fromCharCode(65 + i)" disabled style="width: 48px" />
              <a-input v-model:value="form.options[i]" :placeholder="`选项 ${String.fromCharCode(65 + i)}`" />
            </div>
            <a-button size="small" @click="form.options.push('')" :disabled="form.options.length >= 6">
              <PlusOutlined /> 添加选项
            </a-button>
          </a-form-item>
          <a-form-item label="正确答案" required>
            <!-- 单选：选一个字母 -->
            <a-radio-group v-if="form.type === 1" v-model:value="form.answer">
              <a-radio v-for="(opt, i) in form.options.filter(o => o.trim())" :key="i"
                :value="String.fromCharCode(65 + i)">{{ String.fromCharCode(65 + i) }}</a-radio>
            </a-radio-group>
            <!-- 多选：勾选多个字母 -->
            <a-checkbox-group v-else v-model:value="form.multiAnswers">
              <a-checkbox v-for="(opt, i) in form.options.filter(o => o.trim())" :key="i"
                :value="String.fromCharCode(65 + i)">{{ String.fromCharCode(65 + i) }}</a-checkbox>
            </a-checkbox-group>
          </a-form-item>
        </template>

        <!-- 判断题答案 -->
        <a-form-item v-if="form.type === 3" label="正确答案" required>
          <a-radio-group v-model:value="form.judgeAnswer">
            <a-radio value="对">对</a-radio>
            <a-radio value="错">错</a-radio>
          </a-radio-group>
        </a-form-item>

        <!-- 简答题参考答案 -->
        <a-form-item v-if="form.type === 4" label="参考答案" required>
          <a-textarea v-model:value="form.answer" :rows="3" placeholder="简答题参考答案（阶段四由 AI 辅助批改）" />
        </a-form-item>

        <a-form-item label="解析">
          <a-textarea v-model:value="form.analysis" :rows="2" placeholder="答案解析（选填）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
