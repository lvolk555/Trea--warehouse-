<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { errorBook, markMastered } from '../../api/exam'
import { myCourses } from '../../api/course'

const message = useMessage()

const loading = ref(false)
const items = ref([])
const courses = ref([])
const filterCourse = ref(null)

// 错题详情弹窗
const detailShow = ref(false)
const detailItem = ref(null)

// 下拉分组：当前展开的课程名（空表示全部折叠）
const expandedCourse = ref('')

function toggleCourse(name) {
  expandedCourse.value = expandedCourse.value === name ? '' : name
}

// 按课程归类
const grouped = computed(() => {
  const map = {}
  for (const item of items.value) {
    const key = item.courseTitle || '其他课程'
    if (!map[key]) map[key] = []
    map[key].push(item)
  }
  return map
})

const optionLetters = (options) => (options || []).map((_, i) => String.fromCharCode(65 + i))

const typeText = (t) => ({ 1: '单选', 2: '多选', 3: '判断', 4: '简答' }[t] || '题目')
const typeTag = (t) => ({ 1: 'info', 2: 'warning', 3: 'success', 4: 'default' }[t] || 'default')

// 选项显示标签：判断题直接显示"对/错"，选择/简答显示字母前缀
function optionLabel(item, oi) {
  if (item.type === 3) return item.options[oi]
  return `${optionLetters(item.options)[oi]}. ${item.options[oi]}`
}
function isCorrectOpt(item, oi) {
  const opt = item.options[oi]
  if (item.type === 3) return item.answer === opt
  return item.answer?.includes(optionLetters(item.options)[oi])
}
function isWrongOpt(item, oi) {
  const opt = item.options[oi]
  if (item.type === 3) return item.studentAnswer === opt && item.answer !== opt
  const letter = optionLetters(item.options)[oi]
  return item.studentAnswer?.includes(letter) && !item.answer?.includes(letter)
}

async function loadData() {
  loading.value = true
  try {
    const data = await errorBook(filterCourse.value)
    items.value = data.items || []
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadCourses() {
  try {
    courses.value = await myCourses()
  } catch { /* ignore */ }
}

function openDetail(item) {
  detailItem.value = item
  detailShow.value = true
}

async function handleMastered(item) {
  try {
    await markMastered(item.recordId)
    message.success('已标记掌握，该题已从错题本移除')
    detailShow.value = false
    loadData()
  } catch (e) {
    message.error(e.message)
  }
}

onMounted(() => {
  loadCourses()
  loadData()
})
</script>

<template>
  <div>
    <n-space justify="space-between" align="center" style="margin-bottom: 4px">
      <n-h2 style="margin: 0">错题本</n-h2>
      <n-select v-model:value="filterCourse" clearable placeholder="按课程筛选" style="width: 220px"
        :options="courses.map(c => ({ label: c.title, value: c.id }))" @update:value="loadData" />
    </n-space>
    <n-text depth="3">按课程分组查看错题，点击题目查看详细题目与讲解</n-text>

    <n-spin :show="loading">
      <n-empty v-if="!loading && items.length === 0" description="暂无错题，继续保持" style="margin: 60px 0" />

      <!-- 按课程下拉分组 -->
      <div v-else class="course-groups">
        <div v-for="(list, courseTitle) in grouped" :key="courseTitle" class="course-group">
          <div class="course-header" @click="toggleCourse(courseTitle)">
            <span class="course-name">{{ courseTitle }}</span>
            <n-tag size="small">{{ list.length }} 题</n-tag>
            <span class="arrow" :class="{ open: expandedCourse === courseTitle }">▾</span>
          </div>
          <div v-show="expandedCourse === courseTitle" class="course-body">
            <n-list bordered>
              <n-list-item v-for="item in list" :key="item.recordId" class="q-item" @click="openDetail(item)">
                <div class="q-item-body">
                  <n-tag size="small" :type="typeTag(item.type)">{{ typeText(item.type) }}</n-tag>
                  <span class="q-preview">{{ item.content }}</span>
                </div>
              </n-list-item>
            </n-list>
          </div>
        </div>
      </div>
    </n-spin>

    <!-- 错题详情弹窗 -->
    <n-modal v-model:show="detailShow" preset="card" title="错题详情" style="max-width: 720px">
      <template v-if="detailItem">
        <n-space align="center" size="small" style="margin-bottom: 12px">
          <n-tag size="small" :type="typeTag(detailItem.type)">{{ typeText(detailItem.type) }}</n-tag>
          <n-tag size="small" :bordered="false">{{ detailItem.chapterTitle }}</n-tag>
          <n-text depth="3" style="font-size: 12px">{{ detailItem.createTime }}</n-text>
        </n-space>
        <div class="q-content">{{ detailItem.content }}</div>

        <div v-if="detailItem.options?.length" class="options">
          <div v-for="(opt, oi) in detailItem.options" :key="oi"
            :class="{ 'opt-correct': isCorrectOpt(detailItem, oi), 'opt-wrong': isWrongOpt(detailItem, oi) }">
            {{ optionLabel(detailItem, oi) }}
          </div>
        </div>

        <div class="answer-zone">
          <div><span class="answer-label">我的答案</span>{{ detailItem.studentAnswer || '（未作答）' }}</div>
          <div><span class="answer-label">正确答案</span>{{ detailItem.answer }}</div>
          <div v-if="detailItem.analysis" class="analysis">
            <span class="answer-label">解析</span>{{ detailItem.analysis }}
          </div>
        </div>

        <n-space justify="end" style="margin-top: 20px">
          <n-button @click="detailShow = false">关闭</n-button>
          <n-button type="primary" secondary @click="handleMastered(detailItem)">已掌握，移出错题本</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<style scoped>
.course-groups {
  margin-top: 16px;
}
.course-group {
  border: 1px solid #ebedf2;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
  background: #fff;
}
.course-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  cursor: pointer;
  background: #fafbfc;
  user-select: none;
}
.course-header:hover {
  background: #f0f1f5;
}
.course-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.arrow {
  color: #9ca3af;
  font-size: 14px;
  transition: transform 0.2s;
}
.arrow.open {
  transform: rotate(180deg);
}
.course-body {
  border-top: 1px solid #ebedf2;
}
.q-item {
  cursor: pointer;
}
.q-item:hover {
  background: #f5f6fa;
}
.q-item-body {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.q-preview {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #374151;
}
.q-content {
  font-weight: 500;
  font-size: 15px;
  line-height: 1.7;
  color: #1f2937;
}
.options {
  margin-top: 12px;
}
.options > div {
  padding: 6px 0;
  line-height: 1.6;
}
.opt-correct {
  color: #18a058;
  font-weight: 500;
}
.opt-wrong {
  color: #d03050;
}
.answer-zone {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f8f9fc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
}
.answer-label {
  display: inline-block;
  width: 64px;
  color: #6b7280;
}
.analysis {
  color: #6b7280;
  line-height: 1.7;
}
</style>