<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { courseDetail, enrollCourse } from '../../api/course'
import { courseComments, publishComment } from '../../api/points'
import ExchangeModal from '../../components/ExchangeModal.vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(false)
const course = ref(null)
const enrolling = ref(false)

// 积分兑换弹窗
const exchangeTarget = ref(null)
const exchangeVisible = ref(false)

// 评论区
const comments = ref([])
const commentInput = ref('')
const publishing = ref(false)

async function loadDetail() {
  loading.value = true
  try {
    course.value = await courseDetail(route.params.courseId)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  try {
    comments.value = await courseComments(route.params.courseId)
  } catch (e) {
    /* 评论加载失败不阻断页面 */
  }
}

async function handleEnroll() {
  if (course.value.priceType === 2) {
    exchangeTarget.value = course.value
    exchangeVisible.value = true
    return
  }
  enrolling.value = true
  try {
    await enrollCourse(course.value.id)
    message.success('选课成功，开始学习吧')
    await loadDetail()
  } catch (e) {
    message.error(e.message)
  } finally {
    enrolling.value = false
  }
}

function onExchanged() {
  loadDetail()
  loadComments()
}

async function handlePublishComment() {
  if (!commentInput.value.trim()) {
    message.warning('评论内容不能为空')
    return
  }
  publishing.value = true
  try {
    await publishComment(course.value.id, commentInput.value)
    message.success('评论已提交，审核通过后展示')
    commentInput.value = ''
    await loadComments()
  } catch (e) {
    message.error(e.message)
  } finally {
    publishing.value = false
  }
}

function goStudy(video) {
  if (!course.value.enrolled) {
    message.warning('请先选课')
    return
  }
  router.push(`/study/${course.value.id}/${video.id}`)
}

function formatDuration(sec) {
  const s = Number(sec || 0)
  if (s < 60) return `${s} 秒`
  const m = Math.floor(s / 60)
  if (s % 60 === 0) return `${m} 分钟`
  return `${m} 分 ${s % 60} 秒`
}

onMounted(() => {
  loadDetail()
  loadComments()
})
</script>

<template>
  <n-spin :show="loading">
    <div v-if="course">
      <!-- 课程头部 -->
      <n-card>
        <div class="head">
          <img :src="course.cover" class="cover" />
          <div class="info">
            <h1>{{ course.title }}</h1>
            <n-space align="center" style="margin: 8px 0">
              <n-tag type="info">{{ course.category }}</n-tag>
              <n-tag v-if="course.priceType === 1" type="success">免费</n-tag>
              <n-tag v-else type="warning">{{ course.pointsPrice }} 积分兑换</n-tag>
              <span class="muted">授课：{{ course.teacherName }}</span>
              <span class="muted">共 {{ course.videoCount }} 个小节</span>
            </n-space>
            <p class="desc">{{ course.description }}</p>
            <n-space>
              <n-button v-if="!course.enrolled" type="primary" size="large" :loading="enrolling" @click="handleEnroll">
                {{ course.priceType === 2 ? '积分兑换' : '免费选课' }}
              </n-button>
              <n-button v-else type="primary" size="large" @click="router.push('/my-courses')">
                已选课 · 完成度 {{ course.progress }}%
              </n-button>
            </n-space>
          </div>
        </div>
      </n-card>

      <!-- 章节目录（视频小节 / 文章小节） -->
      <n-card title="课程目录" style="margin-top: 16px">
        <n-collapse>
          <n-collapse-item v-for="chapter in course.chapters" :key="chapter.id" :title="chapter.title">
            <div v-for="video in chapter.videos" :key="video.id" class="video-row" @click="goStudy(video)">
              <span class="video-title">
                <span v-if="video.sectionType === 2" class="article-icon">📄</span>
                <span v-else class="play-icon">▶</span>
                {{ video.title }}
              </span>
              <span v-if="video.sectionType !== 2" class="muted">{{ formatDuration(video.duration) }}</span>
              <n-tag v-else size="small" :bordered="false" type="info">图文</n-tag>
              <n-tag v-if="video.finished" size="small" type="success">已完成</n-tag>
            </div>
          </n-collapse-item>
        </n-collapse>
        <n-empty v-if="!course.chapters || course.chapters.length === 0" description="暂无章节" />
      </n-card>

      <!-- 课程评论区 -->
      <n-card title="课程评论" style="margin-top: 16px">
        <div class="comment-input">
          <n-input v-model:value="commentInput" type="textarea" placeholder="说说你对这门课程的看法（审核通过后展示）" :rows="2" />
          <n-button type="primary" :loading="publishing" @click="handlePublishComment">发表评论</n-button>
        </div>
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <n-avatar round size="small">{{ (c.nickname || '匿')[0] }}</n-avatar>
          <div class="comment-body">
            <div class="comment-head">
              <span class="comment-name">{{ c.nickname }}</span>
              <span class="muted">{{ c.createTime }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
          </div>
        </div>
        <n-empty v-if="comments.length === 0" description="还没有评论，来抢沙发" />
      </n-card>
    </div>
  </n-spin>

  <!-- 积分兑换弹窗：选择优惠券抵扣 -->
  <ExchangeModal v-model:show="exchangeVisible" :course="exchangeTarget" @success="onExchanged" />
</template>

<style scoped>
.head {
  display: flex;
  gap: 24px;
}
.cover {
  width: 360px;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}
.info h1 {
  font-size: 22px;
}
.desc {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 16px;
}
.muted {
  color: #9ca3af;
  font-size: 13px;
}
.video-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 6px;
  cursor: pointer;
}
.video-row:hover {
  background: #f5f6fa;
}
.video-title {
  flex: 1;
}
.play-icon {
  color: #6366f1;
  font-size: 12px;
  margin-right: 6px;
}
.article-icon {
  font-size: 13px;
  margin-right: 6px;
}
.comment-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 20px;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-body {
  flex: 1;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.comment-name {
  font-weight: 600;
  font-size: 13px;
}
.comment-content {
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
}
.article-content {
  line-height: 1.9;
  color: #374151;
  font-size: 15px;
  word-break: break-word;
}
.article-content :deep(h1) { font-size: 24px; margin: 18px 0 10px; }
.article-content :deep(h2) { font-size: 20px; margin: 16px 0 8px; }
.article-content :deep(h3) { font-size: 17px; margin: 14px 0 6px; }
.article-content :deep(pre) {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}
.article-content :deep(code) {
  background: #f6f8fa;
  padding: 2px 5px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
}
.article-content :deep(pre code) { background: transparent; padding: 0; }
.article-content :deep(blockquote) {
  border-left: 3px solid #d0d7de;
  margin: 12px 0;
  padding: 4px 14px;
  color: #57606a;
}
.article-content :deep(ul), .article-content :deep(ol) { padding-left: 24px; margin: 8px 0; }
.article-content :deep(img) { max-width: 100%; border-radius: 6px; }
.article-content :deep(a) { color: #6366f1; }
@media (max-width: 768px) {
  .head {
    flex-direction: column;
    gap: 16px;
  }
  .cover {
    width: 100%;
    height: auto;
  }
}
</style>
