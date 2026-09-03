<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { myScores } from '../../api/exam'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const scores = ref([])

/** 查看该次考试的题目与作答明细 */
function viewDetail(recordId) {
  router.push(`/exam-result/${recordId}`)
}

async function loadData() {
  loading.value = true
  try {
    scores.value = await myScores()
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

onMounted(loadData)
</script>

<template>
  <div>
    <n-h2 style="margin-bottom: 4px">我的成绩</n-h2>
    <n-text depth="3">历史考试记录</n-text>

    <n-spin :show="loading">
      <n-list bordered style="margin-top: 16px">
        <n-list-item v-for="s in scores" :key="s.recordId">
          <n-thing :title="s.examTitle">
            <template #description>
              <n-space size="small">
                <n-tag size="small" type="info">{{ s.courseTitle }}</n-tag>
                <n-text depth="3" style="font-size: 12px">{{ s.submitTime }}</n-text>
              </n-space>
            </template>
          </n-thing>
          <template #suffix>
            <n-space vertical align="end" :size="8">
              <n-tag :type="scoreType(s.score)" size="large" round>{{ s.score }} 分</n-tag>
              <n-button size="small" tertiary type="info" @click="viewDetail(s.recordId)">查看试题</n-button>
            </n-space>
          </template>
        </n-list-item>
      </n-list>
      <n-empty v-if="!loading && scores.length === 0" description="还没有考试记录" style="margin: 60px 0" />
    </n-spin>
  </div>
</template>
