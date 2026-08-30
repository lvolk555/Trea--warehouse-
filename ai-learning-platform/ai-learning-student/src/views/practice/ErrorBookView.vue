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
  } catch {
    // 课程列表加载失败不影响错题本展示
  }
}

async function handleMastered(item) {
  try {
    await markMastered(item.recordId)
    message.success('已标记掌握，该题已从错题本移除')
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
    <n-text depth="3">练习中答错的题目自动收录，标记掌握后移除</n-text>

    <n-spin :show="loading">
      <div v-if="items.length === 0 && !loading" style="margin: 60px 0">
        <n-empty description="暂无错题，继续保持" />
      </div>

      <div v-for="(list, courseTitle) in grouped" :key="courseTitle" class="course-group">
        <n-h4 prefix="bar">{{ courseTitle }}（{{ list.length }} 题）</n-h4>
        <n-card v-for="item in list" :key="item.recordId" size="small" style="margin-bottom: 12px">
          <n-space vertical size="small">
            <n-space align="center">
              <n-tag size="small" :type="item.type === 1 ? 'info' : item.type === 2 ? 'warning' : 'success'">
                {{ item.type === 1 ? '单选' : item.type === 2 ? '多选' : '判断' }}
              </n-tag>
              <n-tag size="small" :bordered="false">{{ item.chapterTitle }}</n-tag>
              <n-text depth="3" style="font-size: 12px">{{ item.createTime }}</n-text>
            </n-space>
            <div class="q-content">{{ item.content }}</div>
            <div v-if="item.options?.length">
              <div v-for="(opt, oi) in item.options" :key="oi"
                :class="{
                  'opt-correct': item.answer?.includes(optionLetters(item.options)[oi]),
                  'opt-wrong': item.studentAnswer?.includes(optionLetters(item.options)[oi]) && !item.answer?.includes(optionLetters(item.options)[oi])
                }">
                {{ optionLetters(item.options)[oi] }}. {{ opt }}
              </div>
            </div>
            <n-space size="small">
              <n-text type="error">我的答案：{{ item.studentAnswer }}</n-text>
              <n-text type="success">正确答案：{{ item.answer }}</n-text>
            </n-space>
            <n-text v-if="item.analysis" depth="3">解析：{{ item.analysis }}</n-text>
            <div>
              <n-button size="small" type="primary" secondary @click="handleMastered(item)">
                已掌握，移出错题本
              </n-button>
            </div>
          </n-space>
        </n-card>
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.course-group {
  margin-top: 20px;
}
.q-content {
  font-weight: 500;
  line-height: 1.7;
}
.opt-correct {
  color: #18a058;
  font-weight: 500;
}
.opt-wrong {
  color: #d03050;
}
</style>
