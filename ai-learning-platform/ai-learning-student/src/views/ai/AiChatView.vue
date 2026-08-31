<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { aiSessions, aiMessages, deleteAiSession } from '../../api/ai'
import { myCourses } from '../../api/course'

const message = useMessage()
const dialog = useDialog()

// 会话列表
const sessions = ref([])
const currentSession = ref(null)
const messages = ref([]) // {role, content}
const courses = ref([])
const selectedCourse = ref(null)

// 输入与流式状态
const question = ref('')
const streaming = ref(false)
const chatBoxRef = ref(null)

async function loadSessions() {
  try {
    sessions.value = await aiSessions()
  } catch (e) {
    message.error(e.message)
  }
}

async function loadCourses() {
  try {
    courses.value = await myCourses()
  } catch {
    // 课程加载失败不影响通用答疑
  }
}

async function openSession(session) {
  currentSession.value = session.id
  selectedCourse.value = session.courseId
  try {
    const list = await aiMessages(session.id)
    messages.value = list.map(m => ({ role: m.role, content: m.content }))
    await scrollToBottom()
  } catch (e) {
    message.error(e.message)
  }
}

function newSession() {
  currentSession.value = null
  messages.value = []
}

function handleDeleteSession(session) {
  dialog.warning({
    title: '删除会话',
    content: '确定删除该会话及其全部消息吗？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await deleteAiSession(session.id)
      message.success('已删除')
      if (currentSession.value === session.id) newSession()
      loadSessions()
    }
  })
}

async function scrollToBottom() {
  await nextTick()
  if (chatBoxRef.value) {
    chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
  }
}

/**
 * 提问：fetch + ReadableStream 消费 SSE 流，实现打字机效果
 */
async function sendQuestion() {
  const q = question.value.trim()
  if (!q) return
  if (streaming.value) return

  streaming.value = true
  question.value = ''
  messages.value.push({ role: 'user', content: q })
  messages.value.push({ role: 'assistant', content: '' })
  const aiIndex = messages.value.length - 1
  await scrollToBottom()

  try {
    const token = localStorage.getItem('student_token')
    const resp = await fetch('/api/student/ai/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        sessionId: currentSession.value,
        courseId: selectedCourse.value,
        question: q
      })
    })
    if (!resp.ok || !resp.body) {
      throw new Error('AI 服务请求失败')
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 以空行分隔事件，逐行解析 data:
      const events = buffer.split('\n\n')
      buffer = events.pop() // 最后一段可能不完整，留到下次
      for (const event of events) {
        for (const line of event.split('\n')) {
          if (!line.startsWith('data:')) continue
          const data = line.slice(5).trim()
          if (!data) continue
          // 首帧：会话 ID
          if (data.startsWith('[SESSION:')) {
            currentSession.value = Number(data.slice(9, -1))
            continue
          }
          messages.value[aiIndex].content += data
          scrollToBottom()
        }
      }
    }
    loadSessions()
  } catch (e) {
    messages.value[aiIndex].content = 'AI 服务暂时不可用，请稍后重试。'
    message.error(e.message || 'AI 服务异常')
  } finally {
    streaming.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  loadSessions()
  loadCourses()
})
</script>

<template>
  <div class="ai-chat-page">
    <!-- 左侧会话列表 -->
    <n-card size="small" class="session-panel">
      <template #header>
        <n-space justify="space-between" align="center">
          <n-text strong>AI 答疑</n-text>
          <n-button size="small" type="primary" @click="newSession">新会话</n-button>
        </n-space>
      </template>
      <div v-for="s in sessions" :key="s.id" class="session-item"
        :class="{ active: currentSession === s.id }" @click="openSession(s)">
        <div class="session-title">{{ s.title }}</div>
        <n-space justify="space-between" align="center">
          <n-text depth="3" style="font-size: 12px">{{ s.createTime }}</n-text>
          <n-button quaternary size="tiny" @click.stop="handleDeleteSession(s)">删除</n-button>
        </n-space>
      </div>
      <n-empty v-if="sessions.length === 0" size="small" description="暂无会话" style="margin-top: 24px" />
    </n-card>

    <!-- 右侧对话区 -->
    <n-card class="chat-panel">
      <!-- 课程上下文选择 -->
      <n-space align="center" style="margin-bottom: 12px">
        <n-text depth="2">答疑课程：</n-text>
        <n-select v-model:value="selectedCourse" clearable placeholder="通用答疑（不绑定课程）"
          class="course-select" size="small"
          :options="courses.map(c => ({ label: c.title, value: c.id }))" />
      </n-space>

      <!-- 消息区 -->
      <div ref="chatBoxRef" class="chat-box">
        <div v-if="messages.length === 0" class="empty-tip">
          <n-text depth="3">向 AI 助教提问吧，选择课程后回答会更有针对性～</n-text>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
          <div class="avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">
            <span>{{ m.content }}</span>
            <span v-if="streaming && i === messages.length - 1 && m.role === 'assistant'" class="cursor">▍</span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-row">
        <div class="input-wrap">
          <n-input v-model:value="question" type="textarea" :rows="2" placeholder="输入你的问题，Enter 发送"
            :disabled="streaming" @keydown.enter.prevent="sendQuestion" />
        </div>
        <n-button class="send-btn" type="primary" :loading="streaming" :disabled="!question.trim()" @click="sendQuestion">
          {{ streaming ? 'AI 回答中…' : '发送' }}
        </n-button>
      </div>
    </n-card>
  </div>
</template>

<style scoped>
.ai-chat-page {
  display: flex;
  gap: 16px;
  height: calc(100vh - 140px);
}
.session-panel {
  width: 260px;
  flex-shrink: 0;
  overflow-y: auto;
}
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.input-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  margin-top: 12px;
}
.input-wrap {
  flex: 1;
  min-width: 0;
}
.send-btn {
  flex-shrink: 0;
  height: 40px;
  min-width: 88px;
}
.course-select {
  width: 260px;
  min-width: 160px;
  flex-shrink: 0;
}
.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 6px;
}
.session-item:hover {
  background: #f5f5f5;
}
.session-item.active {
  background: #eef2ff;
}
.session-title {
  font-size: 13px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
  min-height: 300px;
}
.empty-tip {
  text-align: center;
  margin-top: 80px;
}
.msg-row {
  display: flex;
  margin-bottom: 16px;
  gap: 10px;
}
.msg-row.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.msg-row.user .avatar {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 10px;
  background: #f5f5f5;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-row.user .bubble {
  background: #eef2ff;
}
.cursor {
  animation: blink 1s infinite;
}
@keyframes blink {
  50% { opacity: 0; }
}
@media (max-width: 768px) {
  .ai-chat-page {
    flex-direction: column;
    height: auto;
  }
  .session-panel {
    width: 100%;
    max-height: 180px;
  }
  .chat-panel {
    min-height: 65vh;
  }
  .course-select {
    width: 100%;
  }
}
</style>
