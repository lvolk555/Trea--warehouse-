<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { RobotOutlined, EditOutlined } from '@ant-design/icons-vue'
import * as aiApi from '../../api/ai'

const loading = ref(false)
const grades = ref([])

// 批改结果缓存：answerId → AiGradeVO
const results = ref({})
const gradingIds = ref([])

// 改分弹窗
const adjustVisible = ref(false)
const adjustForm = ref({ answerId: null, score: 0, comment: '' })
const adjusting = ref(false)

async function loadData() {
  loading.value = true
  try {
    grades.value = await aiApi.pendingGrades()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleGrade(item) {
  gradingIds.value.push(item.answerId)
  try {
    const result = await aiApi.aiGrade(item.answerId)
    results.value[item.answerId] = result
    message.success(`AI 评分：${result.aiScore} 分`)
    loadData()
  } catch (e) {
    message.error(e.message)
  } finally {
    gradingIds.value = gradingIds.value.filter(id => id !== item.answerId)
  }
}

function openAdjust(item) {
  const result = results.value[item.answerId]
  adjustForm.value = {
    answerId: item.answerId,
    score: result?.aiScore ?? 5,
    comment: result?.aiComment ?? ''
  }
  adjustVisible.value = true
}

async function handleAdjust() {
  adjusting.value = true
  try {
    const result = await aiApi.adjustScore(
      adjustForm.value.answerId,
      adjustForm.value.score,
      adjustForm.value.comment
    )
    results.value[adjustForm.value.answerId] = result
    message.success(`已改分为 ${result.aiScore} 分，考试总分已重算`)
    adjustVisible.value = false
    loadData()
  } catch (e) {
    message.error(e.message)
  } finally {
    adjusting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div>
    <a-card :bordered="false">
      <template #title>
        <a-space><RobotOutlined /> AI 智能批改</a-space>
      </template>
      <a-alert type="info" show-icon style="margin-bottom: 16px"
        message="考试中学生的简答题由 AI 评分（0-10 分）并给出建议，教师可采纳或手动改分；改分后考试总分自动重算。" />

      <a-spin :spinning="loading">
        <a-empty v-if="grades.length === 0" description="暂无待批改的简答题" />
        <div v-for="item in grades" :key="item.answerId" class="grade-item">
          <div class="grade-header">
            <a-tag color="orange">简答题</a-tag>
            <a-tag>{{ item.examTitle }}</a-tag>
            <a-tag color="blue">学生：{{ item.studentName }}</a-tag>
          </div>
          <div class="q-content">{{ item.questionContent }}</div>
          <a-descriptions size="small" :column="1" bordered style="margin-top: 8px">
            <a-descriptions-item label="参考答案">{{ item.referenceAnswer }}</a-descriptions-item>
            <a-descriptions-item label="学生答案">{{ item.studentAnswer || '（未作答）' }}</a-descriptions-item>
          </a-descriptions>

          <!-- 批改结果 -->
          <div v-if="results[item.answerId]" class="grade-result">
            <a-space>
              <a-tag color="green">AI 评分：{{ results[item.answerId].aiScore }} / 10</a-tag>
              <a-tag v-if="results[item.answerId].examScore" color="blue">
                考试总分：{{ results[item.answerId].examScore }}
              </a-tag>
            </a-space>
            <div class="comment">批改建议：{{ results[item.answerId].aiComment }}</div>
          </div>

          <div class="actions">
            <a-space>
              <a-button type="primary" size="small" :loading="gradingIds.includes(item.answerId)"
                @click="handleGrade(item)">
                <RobotOutlined /> {{ results[item.answerId] ? '重新批改' : 'AI 批改' }}
              </a-button>
              <a-button size="small" :disabled="!results[item.answerId]" @click="openAdjust(item)">
                <EditOutlined /> 采纳 / 改分
              </a-button>
            </a-space>
          </div>
        </div>
      </a-spin>
    </a-card>

    <!-- 改分弹窗 -->
    <a-modal v-model:open="adjustVisible" title="采纳 / 改分" :confirm-loading="adjusting"
      ok-text="确认" cancel-text="取消" @ok="handleAdjust">
      <a-form layout="vertical">
        <a-form-item label="评分（0-10）" required>
          <a-input-number v-model:value="adjustForm.score" :min="0" :max="10" :step="0.5" style="width: 100%" />
        </a-form-item>
        <a-form-item label="批改建议">
          <a-textarea v-model:value="adjustForm.comment" :rows="3" placeholder="可修改 AI 给出的建议" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.grade-item {
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 16px;
}
.grade-header {
  margin-bottom: 8px;
}
.q-content {
  font-weight: 500;
  line-height: 1.7;
}
.grade-result {
  margin-top: 12px;
  padding: 10px;
  background: #f6ffed;
  border-radius: 6px;
}
.comment {
  margin-top: 6px;
  color: #666;
  font-size: 13px;
}
.actions {
  margin-top: 12px;
}
</style>
