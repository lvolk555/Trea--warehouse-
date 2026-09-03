<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { examRecordDetail } from '../../api/exam'

const route = useRoute()
const router = useRouter()
const message = useMessage()

// 交卷后通过路由 state 传递判分结果
const result = ref(history.state?.score !== undefined ? history.state : null)
const loading = ref(false)

const optionLetters = (options) => (options || []).map((_, i) => String.fromCharCode(65 + i))

onMounted(async () => {
  if (result.value) return
  // 从“我的成绩”进入：按记录 ID 拉取答题详情
  loading.value = true
  try {
    result.value = await examRecordDetail(route.params.recordId)
  } catch (e) {
    message.error(e.message || '未找到考试结果')
    router.replace('/scores')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <n-spin :show="loading">
    <div v-if="result">
    <!-- 成绩总览 -->
    <n-card style="text-align: center">
      <n-statistic label="考试得分" tabular-nums>
        <n-number-animation :from="0" :to="Number(result.score)" :precision="1" />
      </n-statistic>
      <n-text depth="3">
        共 {{ result.totalCount }} 题，答对 {{ result.correctCount }} 题
      </n-text>
      <div style="margin-top: 16px">
        <n-space justify="center">
          <n-button @click="router.push('/exam')">返回考试列表</n-button>
          <n-button type="primary" @click="router.push('/scores')">我的成绩</n-button>
          <n-button @click="router.push('/error-book')">查看错题本</n-button>
        </n-space>
      </div>
    </n-card>

    <!-- 每题判分明细 -->
    <n-card title="答题详情" style="margin-top: 16px">
      <n-collapse>
        <n-collapse-item v-for="(d, i) in result.details" :key="d.questionId" :name="i">
          <template #header>
            <n-space align="center" style="width: 100%">
              <n-tag :type="d.correct === 1 ? 'success' : 'error'" size="small">
                {{ d.correct === 1 ? '答对' : '答错' }}
              </n-tag>
              <n-text>{{ i + 1 }}. {{ d.content }}</n-text>
            </n-space>
          </template>
          <n-space vertical size="small">
            <n-text depth="2">题型：{{ ['', '单选题', '多选题', '判断题', '简答题'][d.type] }}</n-text>
            <div v-if="d.options?.length">
              <div v-for="(opt, oi) in d.options" :key="oi"
                :class="{ 'opt-correct': d.answer?.includes(optionLetters(d.options)[oi]), 'opt-wrong': d.studentAnswer?.includes?.(optionLetters(d.options)[oi]) && !d.answer?.includes(optionLetters(d.options)[oi]) }">
                {{ optionLetters(d.options)[oi] }}. {{ opt }}
              </div>
            </div>
            <n-text>你的答案：{{ d.studentAnswer || '（未作答）' }}</n-text>
            <n-text type="success">正确答案：{{ d.answer }}</n-text>
            <n-text v-if="d.analysis" depth="3">解析：{{ d.analysis }}</n-text>
          </n-space>
        </n-collapse-item>
      </n-collapse>
    </n-card>
    </div>
  </n-spin>
</template>

<style scoped>
.opt-correct {
  color: #18a058;
  font-weight: 500;
}
.opt-wrong {
  color: #d03050;
}
</style>
