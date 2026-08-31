<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { courseDetail, enrollCourse } from '../../api/course'
import { courseComments, publishComment } from '../../api/points'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(false)
const course = ref(null)
const enrolling = ref(false)

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
    message.info('该课程为积分兑换课程，请到积分中心兑换')
    router.push('/points')
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
              <span class="muted">{{ course.videoCount }} 节视频</span>
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

      <!-- 章节目录 -->
      <n-card title="课程目录" style="margin-top: 16px">
        <n-collapse>
          <n-collapse-item v-for="chapter in course.chapters" :key="chapter.id" :title="chapter.title">
            <div v-for="video in chapter.videos" :key="video.id" class="video-row" @click="goStudy(video)">
              <span class="video-title">
                <span class="play-icon">▶</span>
                {{ video.title }}
              </span>
              <span class="muted">{{ Math.round((video.duration || 0) / 60) }} 分钟</span>
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
</style>
