<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { studentExamList, myScores } from '../../api/exam'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const exams = ref([])
const scores = ref([])

// 考试 ID → 成绩记录（一次考试对应一条记录，取最近一次）
const scoreMap = computed(() => {
  const map = {}
  for (const s of scores.value) {
    if (!map[s.examId]) map[s.examId] = s
  }
  return map
})

async function loadData() {
  loading.value = true
  try {
    const [examData, scoreData] = await Promise.all([studentExamList(), myScores()])
    exams.value = examData
    scores.value = scoreData
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function scoreType(score) {
  const s = Number(score)
  if (s >= 90) return 'success'
  if (s >= 60) return 'warning'
  return 'error'
}

function startExam(exam) {
  if (scoreMap.value[exam.id]) {
    message.info('你已完成该考试，可到「我的成绩」查看分数')
    return
  }
  router.push(`/exam/${exam.id}`)
}

onMounted(loadData)
</script>

<template>
  <div>
    <n-h2 style="margin-bottom: 4px">在线考试</n-h2>
    <n-text depth="3">已选课程的考试列表，点击参加考试（限时自动交卷）</n-text>

    <n-spin :show="loading">
      <n-list hoverable clickable style="margin-top: 16px" bordered>
        <n-list-item v-for="exam in exams" :key="exam.id" @click="startExam(exam)">
          <n-thing :title="exam.title">
            <template #description>
              <n-space size="small">
                <n-tag size="small" type="info">{{ exam.courseTitle }}</n-tag>
                <n-tag size="small">{{ exam.questionIds?.length || 0 }} 题</n-tag>
                <n-tag size="small" type="warning">限时 {{ exam.duration }} 分钟</n-tag>
              </n-space>
            </template>
          </n-thing>
          <template #suffix>
            <n-space v-if="scoreMap[exam.id]" align="center" size="small">
              <n-tag size="small" type="info">已考</n-tag>
              <n-tag :type="scoreType(scoreMap[exam.id].score)" size="large" round>{{ scoreMap[exam.id].score }} 分</n-tag>
            </n-space>
            <n-button v-else type="primary" size="small">参加考试</n-button>
          </template>
        </n-list-item>
      </n-list>
      <n-empty v-if="!loading && exams.length === 0" description="暂无可参加的考试" style="margin: 60px 0" />
    </n-spin>
  </div>
</template>
