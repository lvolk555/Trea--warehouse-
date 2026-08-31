<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { myCourses, courseDetail } from '../../api/course'
import { pickQuestions, submitPractice } from '../../api/exam'

const message = useMessage()

// 第一步：选课程 + 章节
const step = ref('select') // select | quiz | done
const courses = ref([])
const chapters = ref([])
const selectedCourse = ref(null)
const selectedChapter = ref(null)
const loadingCourses = ref(false)

// 第二步：答题
const questions = ref([])
const currentIndex = ref(0)
const myAnswer = ref(null)
const judgeResult = ref(null) // {correct, answer, analysis}
const submitting = ref(false)
const stats = ref({ correct: 0, total: 0 })

const currentQuestion = computed(() => questions.value[currentIndex.value])
const optionLetters = computed(() =>
  (currentQuestion.value?.options || []).map((_, i) => String.fromCharCode(65 + i))
)

async function loadCourses() {
  loadingCourses.value = true
  try {
    courses.value = await myCourses()
  } catch (e) {
    message.error(e.message)
  } finally {
    loadingCourses.value = false
  }
}

async function onCourseChange(courseId) {
  selectedChapter.value = null
  chapters.value = []
  if (!courseId) return
  try {
    const detail = await courseDetail(courseId)
    chapters.value = detail.chapters || []
  } catch (e) {
    message.error(e.message)
  }
}

async function startPractice() {
  if (!selectedChapter.value) {
    return message.warning('请选择要练习的章节')
  }
  try {
    const list = await pickQuestions(selectedChapter.value, 10)
    if (!list.length) {
      return message.info('该章节暂无练习题，请等待老师录入')
    }
    questions.value = list
    currentIndex.value = 0
    myAnswer.value = null
    judgeResult.value = null
    stats.value = { correct: 0, total: 0 }
    step.value = 'quiz'
  } catch (e) {
    message.error(e.message)
  }
}

async function handleSubmit() {
  if (myAnswer.value === null || myAnswer.value === '') {
    return message.warning('请先作答')
  }
  submitting.value = true
  try {
    // 多选题答案为数组，转为字符串提交（如 ["A","B"] → "AB"）
    const answerText = Array.isArray(myAnswer.value) ? myAnswer.value.join('') : String(myAnswer.value)
    const result = await submitPractice({
      questionId: currentQuestion.value.id,
      studentAnswer: answerText
    })
    judgeResult.value = result
    stats.value.total++
    if (result.correct) stats.value.correct++
  } catch (e) {
    message.error(e.message)
  } finally {
    submitting.value = false
  }
}

function nextQuestion() {
  if (currentIndex.value + 1 >= questions.value.length) {
    step.value = 'done'
    return
  }
  currentIndex.value++
  myAnswer.value = null
  judgeResult.value = null
}

function restart() {
  step.value = 'select'
  questions.value = []
}

onMounted(loadCourses)
</script>

<template>
  <div class="practice-page">
    <n-h2 style="margin-bottom: 4px">章节练习</n-h2>
    <n-text depth="3">选择课程章节随机抽题，客观题即时判分，错题自动进入错题本</n-text>

    <!-- 选择课程章节 -->
    <n-card v-if="step === 'select'" style="margin-top: 20px; max-width: 560px">
      <n-space vertical size="large">
        <div>
          <n-text depth="2" style="display: block; margin-bottom: 8px">选择课程</n-text>
          <n-select v-model:value="selectedCourse" :options="courses.map(c => ({ label: c.title, value: c.id }))"
            placeholder="选择已选课程" :loading="loadingCourses" @update:value="onCourseChange" />
        </div>
        <div>
          <n-text depth="2" style="display: block; margin-bottom: 8px">选择章节</n-text>
          <n-select v-model:value="selectedChapter" :options="chapters.map(c => ({ label: c.title, value: c.id }))"
            placeholder="先选择课程" :disabled="!selectedCourse" />
        </div>
        <n-button type="primary" block @click="startPractice">开始练习（随机 10 题）</n-button>
      </n-space>
    </n-card>

    <!-- 答题区 -->
    <n-card v-else-if="step === 'quiz'" style="margin-top: 20px">
      <template #header>
        <n-space justify="space-between" align="center">
          <n-text>第 {{ currentIndex + 1 }} / {{ questions.length }} 题</n-text>
          <n-text depth="3">答对 {{ stats.correct }} 题</n-text>
        </n-space>
      </template>

      <n-space vertical size="large">
        <n-tag :type="currentQuestion.type === 1 ? 'info' : currentQuestion.type === 2 ? 'warning' : 'success'" size="small">
          {{ currentQuestion.type === 1 ? '单选题' : currentQuestion.type === 2 ? '多选题' : '判断题' }}
        </n-tag>
        <div class="q-content">{{ currentQuestion.content }}</div>

        <!-- 单选 -->
        <n-radio-group v-if="currentQuestion.type === 1" v-model:value="myAnswer" :disabled="!!judgeResult">
          <n-space vertical>
            <n-radio v-for="(opt, i) in currentQuestion.options" :key="i" :value="optionLetters[i]">
              {{ optionLetters[i] }}. {{ opt }}
            </n-radio>
          </n-space>
        </n-radio-group>
        <!-- 判断 -->
        <n-radio-group v-else-if="currentQuestion.type === 3" v-model:value="myAnswer" :disabled="!!judgeResult">
          <n-space>
            <n-radio value="对">对</n-radio>
            <n-radio value="错">错</n-radio>
          </n-space>
        </n-radio-group>
        <!-- 多选 -->
        <n-checkbox-group v-else v-model:value="myAnswer" :disabled="!!judgeResult">
          <n-space vertical>
            <n-checkbox v-for="(opt, i) in currentQuestion.options" :key="i" :value="optionLetters[i]">
              {{ optionLetters[i] }}. {{ opt }}
            </n-checkbox>
          </n-space>
        </n-checkbox-group>

        <!-- 判分结果 -->
        <n-alert v-if="judgeResult" :type="judgeResult.correct ? 'success' : 'error'" :title="judgeResult.correct ? '回答正确' : '回答错误'">
          <div>正确答案：{{ judgeResult.answer }}</div>
          <div v-if="judgeResult.analysis" style="margin-top: 6px">解析：{{ judgeResult.analysis }}</div>
        </n-alert>

        <n-space>
          <n-button v-if="!judgeResult" type="primary" :loading="submitting" @click="handleSubmit">提交答案</n-button>
          <n-button v-else type="primary" @click="nextQuestion">
            {{ currentIndex + 1 >= questions.length ? '完成练习' : '下一题' }}
          </n-button>
        </n-space>
      </n-space>
    </n-card>

    <!-- 练习完成 -->
    <n-card v-else style="margin-top: 20px; max-width: 560px; text-align: center">
      <n-result status="success" title="练习完成"
        :description="`共 ${stats.total} 题，答对 ${stats.correct} 题，正确率 ${stats.total ? Math.round(stats.correct * 100 / stats.total) : 0}%`">
        <template #footer>
          <n-space justify="center">
            <n-button @click="restart">再练一组</n-button>
            <n-button type="primary" @click="$router.push('/error-book')">查看错题本</n-button>
          </n-space>
        </template>
      </n-result>
    </n-card>
  </div>
</template>

<style scoped>
.q-content {
  font-size: 16px;
  font-weight: 500;
  line-height: 1.7;
}
</style>
