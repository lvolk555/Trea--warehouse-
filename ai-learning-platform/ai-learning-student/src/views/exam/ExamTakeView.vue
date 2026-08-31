<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { startExam, submitExam } from '../../api/exam'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const loading = ref(true)
const exam = ref(null)
const answers = reactive({}) // questionId → 答案
const currentIndex = ref(0)
const submitting = ref(false)

// 倒计时
const remainSeconds = ref(0)
let timer = null

const currentQuestion = computed(() => exam.value?.questions?.[currentIndex.value])
const optionLetters = computed(() =>
  (currentQuestion.value?.options || []).map((_, i) => String.fromCharCode(65 + i))
)
const countdownText = computed(() => {
  const m = Math.floor(remainSeconds.value / 60)
  const s = remainSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})
const answeredCount = computed(() =>
  Object.values(answers).filter(a => a !== undefined && a !== null && a !== '').length
)

async function loadExam() {
  try {
    const data = await startExam(route.params.examId)
    exam.value = data
    remainSeconds.value = (data.duration || 60) * 60
    timer = setInterval(() => {
      remainSeconds.value--
      if (remainSeconds.value <= 0) {
        clearInterval(timer)
        message.warning('考试时间到，系统自动交卷')
        doSubmit(true)
      }
    }, 1000)
  } catch (e) {
    message.error(e.message)
    router.push('/exam')
  } finally {
    loading.value = false
  }
}

function confirmSubmit() {
  const unanswered = (exam.value?.questions?.length || 0) - answeredCount.value
  dialog.warning({
    title: '交卷确认',
    content: unanswered > 0 ? `还有 ${unanswered} 题未作答，确定交卷吗？` : '确定交卷吗？交卷后不可修改。',
    positiveText: '交卷',
    negativeText: '继续答题',
    onPositiveClick: () => doSubmit(false)
  })
}

async function doSubmit(auto) {
  if (submitting.value) return
  submitting.value = true
  try {
    // 多选题答案为数组，转为字符串提交（如 ["A","B"] → "AB"）
    const normalized = {}
    for (const [qid, ans] of Object.entries(answers)) {
      normalized[qid] = Array.isArray(ans) ? ans.join('') : ans
    }
    const result = await submitExam({
      examId: exam.value.id,
      answers: normalized
    })
    clearInterval(timer)
    router.replace({ path: `/exam-result/${result.recordId}`, state: result })
  } catch (e) {
    message.error(e.message)
    if (!auto) router.push('/exam')
  } finally {
    submitting.value = false
  }
}

onMounted(loadExam)
onUnmounted(() => timer && clearInterval(timer))
</script>

<template>
  <div>
    <n-spin :show="loading">
      <template v-if="exam">
        <!-- 顶部信息栏 -->
        <n-card size="small" style="position: sticky; top: 0; z-index: 10">
          <n-space justify="space-between" align="center">
            <n-space align="center">
              <n-text strong>{{ exam.title }}</n-text>
              <n-tag size="small">{{ exam.courseTitle }}</n-tag>
            </n-space>
            <n-space align="center">
              <n-tag :type="remainSeconds < 300 ? 'error' : 'warning'" size="large">
                剩余时间 {{ countdownText }}
              </n-tag>
              <n-button type="primary" :loading="submitting" @click="confirmSubmit">交卷</n-button>
            </n-space>
          </n-space>
        </n-card>

        <div class="exam-body">
          <!-- 左侧答题卡 -->
          <n-card size="small" class="answer-card">
            <template #header><n-text depth="2">答题卡</n-text></template>
            <n-space>
              <n-button v-for="(q, i) in exam.questions" :key="q.id" size="small"
                :type="answers[q.id] ? 'primary' : 'default'"
                :secondary="i === currentIndex"
                @click="currentIndex = i">
                {{ i + 1 }}
              </n-button>
            </n-space>
            <n-text depth="3" style="display: block; margin-top: 12px">
              已答 {{ answeredCount }} / {{ exam.questions.length }} 题
            </n-text>
          </n-card>

          <!-- 右侧题目区 -->
          <n-card class="question-card">
            <n-space vertical size="large">
              <n-space align="center">
                <n-text depth="2">{{ currentIndex + 1 }} / {{ exam.questions.length }}</n-text>
                <n-tag size="small" :type="currentQuestion.type === 4 ? 'error' : 'info'">
                  {{ ['', '单选题', '多选题', '判断题', '简答题'][currentQuestion.type] }}
                </n-tag>
              </n-space>
              <div class="q-content">{{ currentQuestion.content }}</div>

              <!-- 单选 -->
              <n-radio-group v-if="currentQuestion.type === 1" v-model:value="answers[currentQuestion.id]">
                <n-space vertical>
                  <n-radio v-for="(opt, i) in currentQuestion.options" :key="i" :value="optionLetters[i]">
                    {{ optionLetters[i] }}. {{ opt }}
                  </n-radio>
                </n-space>
              </n-radio-group>
              <!-- 多选 -->
              <n-checkbox-group v-else-if="currentQuestion.type === 2" v-model:value="answers[currentQuestion.id]">
                <n-space vertical>
                  <n-checkbox v-for="(opt, i) in currentQuestion.options" :key="i" :value="optionLetters[i]">
                    {{ optionLetters[i] }}. {{ opt }}
                  </n-checkbox>
                </n-space>
              </n-checkbox-group>
              <!-- 判断 -->
              <n-radio-group v-else-if="currentQuestion.type === 3" v-model:value="answers[currentQuestion.id]">
                <n-space>
                  <n-radio value="对">对</n-radio>
                  <n-radio value="错">错</n-radio>
                </n-space>
              </n-radio-group>
              <!-- 简答 -->
              <n-input v-else v-model:value="answers[currentQuestion.id]" type="textarea" :rows="5"
                placeholder="请输入你的答案（简答题将由 AI 批改）" />

              <n-space justify="space-between">
                <n-button :disabled="currentIndex === 0" @click="currentIndex--">上一题</n-button>
                <n-button v-if="currentIndex < exam.questions.length - 1" @click="currentIndex++">下一题</n-button>
                <n-button v-else type="primary" :loading="submitting" @click="confirmSubmit">交卷</n-button>
              </n-space>
            </n-space>
          </n-card>
        </div>
      </template>
    </n-spin>
  </div>
</template>

<style scoped>
.exam-body {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  align-items: flex-start;
}
.answer-card {
  width: 240px;
  flex-shrink: 0;
}
.question-card {
  flex: 1;
}
.q-content {
  font-size: 16px;
  font-weight: 500;
  line-height: 1.7;
}
@media (max-width: 768px) {
  .exam-body {
    flex-direction: column;
  }
  .answer-card {
    width: 100%;
  }
}
</style>
