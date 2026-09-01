<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { RobotOutlined, CheckOutlined } from '@ant-design/icons-vue'
import * as aiApi from '../../api/ai'
import * as courseApi from '../../api/course'

const typeMap = {
  1: { text: '单选题', color: 'blue' },
  2: { text: '多选题', color: 'purple' },
  3: { text: '判断题', color: 'cyan' }
}

// 出题表单
const courses = ref([])
const chapters = ref([])
const generating = ref(false)
const form = reactive({
  courseId: null,
  chapterId: null,
  knowledgePoint: '',
  type: 1,
  count: 5
})

// 生成的草稿
const drafts = ref([])
const selectedDrafts = ref([]) // 勾选入库的索引
const saving = ref(false)

async function loadCourses() {
  try {
    const list = await courseApi.teacherCourseList()
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

function onCourseChange(courseId) {
  form.chapterId = null
  const course = courses.value.find(c => c.id === courseId)
  chapters.value = course?.chapters || []
}

async function handleGenerate() {
  if (!form.courseId || !form.chapterId) {
    return message.warning('请选择课程和章节')
  }
  generating.value = true
  drafts.value = []
  selectedDrafts.value = []
  try {
    drafts.value = await aiApi.aiGenerate({
      courseId: form.courseId,
      chapterId: form.chapterId,
      knowledgePoint: form.knowledgePoint,
      type: form.type,
      count: form.count
    })
    // 默认全部勾选
    selectedDrafts.value = drafts.value.map((_, i) => i)
    message.success(`AI 已生成 ${drafts.value.length} 道题目，请审核后入库`)
  } catch (e) {
    message.error(e.message)
  } finally {
    generating.value = false
  }
}

function updateDraft(index, field, value) {
  drafts.value[index][field] = value
}

async function handleSave() {
  if (selectedDrafts.value.length === 0) {
    return message.warning('请至少勾选一道题入库')
  }
  saving.value = true
  try {
    const questions = selectedDrafts.value.map(i => drafts.value[i])
    const count = await aiApi.aiSaveDrafts({
      courseId: form.courseId,
      chapterId: form.chapterId,
      questions
    })
    message.success(`成功入库 ${count} 道题目（来源标记为 AI 生成）`)
    drafts.value = []
    selectedDrafts.value = []
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

onMounted(loadCourses)
</script>

<template>
  <div>
    <a-card :bordered="false">
      <template #title>
        <a-space><RobotOutlined /> AI 智能出题</a-space>
      </template>
      <a-alert type="info" show-icon style="margin-bottom: 16px"
        message="选择课程章节与题型，AI 自动生成题目草稿；教师审核修改后勾选入库，入库题目标记来源为 AI 生成。" />

      <a-form layout="vertical" style="margin-bottom: 16px">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="课程" required>
              <a-select v-model:value="form.courseId" placeholder="选择课程" @change="onCourseChange">
                <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.title }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="章节" required>
              <a-select v-model:value="form.chapterId" placeholder="先选择课程" :disabled="!form.courseId">
                <a-select-option v-for="ch in chapters" :key="ch.id" :value="ch.id">{{ ch.title }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="知识点（选填）">
              <a-input v-model:value="form.knowledgePoint" placeholder="如：循环结构" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="题型">
              <a-radio-group v-model:value="form.type" button-style="solid">
                <a-radio-button v-for="(v, k) in typeMap" :key="k" :value="Number(k)">{{ v.text }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="数量">
              <a-input-number v-model:value="form.count" :min="1" :max="10" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label=" ">
              <a-button type="primary" :loading="generating" @click="handleGenerate">
                <RobotOutlined /> {{ generating ? 'AI 生成中…' : '生成题目' }}
              </a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <!-- 草稿列表 -->
      <template v-if="drafts.length > 0">
        <a-divider>题目草稿（可编辑修改，勾选后入库）</a-divider>
        <a-checkbox-group v-model:value="selectedDrafts" style="width: 100%">
          <div v-for="(draft, i) in drafts" :key="i" class="draft-item">
            <a-checkbox :value="i" style="align-self: flex-start; margin-top: 4px" />
            <div style="flex: 1">
              <a-tag :color="typeMap[form.type]?.color">{{ typeMap[form.type]?.text }}</a-tag>
              <a-textarea :value="draft.content" :rows="2" style="margin: 8px 0"
                @change="e => updateDraft(i, 'content', e.target.value)" />
              <div v-if="form.type !== 3" style="margin-bottom: 8px">
                <a-input v-for="(opt, oi) in draft.options" :key="oi" :value="opt" size="small"
                  style="margin-bottom: 4px" :addon-before="String.fromCharCode(65 + oi)"
                  @change="e => (draft.options[oi] = e.target.value)" />
              </div>
              <a-space wrap>
                <a-input :value="draft.answer" size="small" style="width: 200px; max-width: 100%" addon-before="答案"
                  @change="e => updateDraft(i, 'answer', e.target.value)" />
                <a-input :value="draft.analysis" size="small" style="width: 320px; max-width: 100%" addon-before="解析"
                  @change="e => updateDraft(i, 'analysis', e.target.value)" />
              </a-space>
            </div>
          </div>
        </a-checkbox-group>
        <div style="margin-top: 16px; text-align: right">
          <a-button type="primary" :loading="saving" @click="handleSave">
            <CheckOutlined /> 勾选入库（{{ selectedDrafts.length }} 题）
          </a-button>
        </div>
      </template>
    </a-card>
  </div>
</template>

<style scoped>
.draft-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
}
</style>
